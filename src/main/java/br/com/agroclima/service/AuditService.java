package br.com.agroclima.service;

import br.com.agroclima.domain.AuditEventEntity;
import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.dto.AuditDtos;
import br.com.agroclima.repository.AuditEventRepository;
import br.com.agroclima.repository.UserRepository;
import br.com.agroclima.security.AgroUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {
    private final AuditEventRepository events;
    private final UserRepository users;

    public AuditService(AuditEventRepository events, UserRepository users) {
        this.events = events;
        this.users = users;
    }

    @Transactional
    public void record(UserEntity actor, String action, String resource, String details) {
        String name = actor == null ? "Serviço" : actor.getFullName();
        String email = actor == null ? "" : actor.getEmail();
        events.save(new AuditEventEntity(actor, name, email, safe(action, 160), safe(resource, 160),
            "API Java", "CONCLUIDO", safe(details, 4000)));
    }

    @Transactional
    public void record(AgroUserPrincipal actor, String action, String resource, String details) {
        UserEntity entity = actor == null ? null : users.getReferenceById(actor.id());
        String name = actor == null ? "Serviço" : actor.fullName();
        String email = actor == null ? "" : actor.email();
        events.save(new AuditEventEntity(entity, name, email, safe(action, 160), safe(resource, 160),
            "API Java", "CONCLUIDO", safe(details, 4000)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuthenticationFailure(String attemptedEmail) {
        String email = safe(attemptedEmail, 254).toLowerCase(java.util.Locale.ROOT);
        String actorName = email.isBlank() ? "Usuário não identificado" : email;
        events.save(new AuditEventEntity(null, safe(actorName, 160), email, "Falha ao autenticar", "Sessão",
            "API Java", "FALHOU", "Credenciais inválidas ou conta indisponível."));
    }

    @Transactional(readOnly = true)
    public List<AuditDtos.AuditEventResponse> latest() {
        return events.findTop500ByOrderByOccurredAtDesc().stream().map(event ->
            new AuditDtos.AuditEventResponse(event.getId(), event.getActorName(), event.getActorEmail(),
                event.getAction(), event.getResource(), event.getOccurredAt(), event.getSource(),
                "CONCLUIDO".equals(event.getStatus()) ? "Concluído" : "Falhou", event.getDetails())).toList();
    }

    private String safe(String value, int maxLength) {
        if (value == null || value.isBlank()) return "";
        String clean = value.strip();
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }
}
