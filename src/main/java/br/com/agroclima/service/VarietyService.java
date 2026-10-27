package br.com.agroclima.service;

import br.com.agroclima.domain.VarietyEntity;
import br.com.agroclima.dto.VarietyDtos;
import br.com.agroclima.exception.ApiException;
import br.com.agroclima.repository.VarietyRepository;
import br.com.agroclima.security.AgroUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VarietyService {
    private final VarietyRepository varieties;
    private final AuditService audit;

    public VarietyService(VarietyRepository varieties, AuditService audit) {
        this.varieties = varieties;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<VarietyDtos.VarietyResponse> list(boolean includeInactive) {
        List<VarietyEntity> results = includeInactive ? varieties.findAllByOrderByNameAsc() : varieties.findAllByActiveTrueOrderByNameAsc();
        return results.stream().map(this::toResponse).toList();
    }

    @Transactional
    public VarietyDtos.VarietyResponse create(VarietyDtos.CreateVarietyRequest request, AgroUserPrincipal actor) {
        String name = request.name().trim();
        if (varieties.existsByNameIgnoreCase(name)) throw ApiException.conflict("Este tipo de uva já está cadastrado.");
        VarietyEntity saved = varieties.save(new VarietyEntity(name, request.category()));
        audit.record(actor, "Cadastrou um tipo de uva", "Variedades", name + ".");
        return toResponse(saved);
    }

    @Transactional
    public VarietyDtos.VarietyResponse setActive(java.util.UUID id, boolean active, AgroUserPrincipal actor) {
        VarietyEntity variety = varieties.findById(id).orElseThrow(() -> ApiException.notFound("Tipo de uva não encontrado."));
        if (active) variety.activate(); else variety.deactivate();
        VarietyEntity saved = varieties.save(variety);
        audit.record(actor, active ? "Ativou um tipo de uva" : "Desativou um tipo de uva", "Variedades", saved.getName() + ".");
        return toResponse(saved);
    }

    private VarietyDtos.VarietyResponse toResponse(VarietyEntity entity) {
        return new VarietyDtos.VarietyResponse(entity.getId(), entity.getName(), entity.getCategory(), entity.isActive());
    }
}
