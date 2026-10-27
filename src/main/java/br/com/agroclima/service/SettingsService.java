package br.com.agroclima.service;

import br.com.agroclima.config.AppProperties;
import br.com.agroclima.domain.AppSettingEntity;
import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.exception.ApiException;
import br.com.agroclima.repository.AppSettingRepository;
import br.com.agroclima.repository.UserRepository;
import br.com.agroclima.repository.VarietyRepository;
import br.com.agroclima.security.AgroUserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
import java.util.Set;

@Service
public class SettingsService {
    private static final Set<String> SECTIONS = Set.of("general", "notifications", "appearance", "security");
    private final AppSettingRepository settings;
    private final UserRepository users;
    private final VarietyRepository varieties;
    private final ObjectMapper mapper;
    private final AuditService audit;
    private final AppProperties properties;

    public SettingsService(AppSettingRepository settings, UserRepository users, VarietyRepository varieties,
                           ObjectMapper mapper, AuditService audit, AppProperties properties) {
        this.settings = settings;
        this.users = users;
        this.varieties = varieties;
        this.mapper = mapper;
        this.audit = audit;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public JsonNode get() {
        ObjectNode result = defaults();
        for (String section : SECTIONS) {
            settings.findByKey(section).ifPresent(entity -> {
                if (entity.getJsonValue() != null && entity.getJsonValue().isObject()) result.set(section, entity.getJsonValue());
            });
        }
        return result;
    }

    @Transactional
    public JsonNode update(JsonNode patch, AgroUserPrincipal actor) {
        if (patch == null || !patch.isObject()) throw ApiException.badRequest("As configurações devem ser enviadas como um objeto JSON.");
        Iterator<String> names = patch.fieldNames();
        while (names.hasNext()) {
            String section = names.next();
            if (!SECTIONS.contains(section)) throw ApiException.badRequest("Seção de configuração desconhecida.");
            if (!patch.get(section).isObject()) throw ApiException.badRequest("Cada seção deve ser um objeto JSON.");
        }

        ObjectNode current = (ObjectNode) get();
        for (String section : SECTIONS) {
            JsonNode sectionPatch = patch.get(section);
            if (sectionPatch == null) continue;
            ObjectNode merged = (ObjectNode) current.get(section).deepCopy();
            merge(merged, sectionPatch);
            validateSection(section, merged);
            UserEntity updater = users.getReferenceById(actor.id());
            AppSettingEntity entity = settings.findByKey(section).orElseGet(() -> new AppSettingEntity(section, merged, updater));
            entity.updateValue(merged, updater);
            settings.save(entity);
            current.set(section, merged);
        }
        audit.record(actor, "Atualizou configurações", "Configurações", "Preferências do sistema atualizadas.");
        return current;
    }

    private void merge(ObjectNode target, JsonNode patch) {
        patch.fields().forEachRemaining(entry -> {
            JsonNode existing = target.get(entry.getKey());
            JsonNode incoming = entry.getValue();
            if (existing != null && existing.isObject() && incoming.isObject()) {
                merge((ObjectNode) existing, incoming);
            } else {
                target.set(entry.getKey(), incoming.deepCopy());
            }
        });
    }

    private void validateSection(String section, ObjectNode data) {
        if (section.equals("general")) {
            validateText(data, "systemName", 48);
            validateText(data, "region", 120);
            validateText(data, "institutionalInfo", 240);
            JsonNode variety = data.get("defaultVariety");
            if (variety == null || !variety.isTextual() || varieties.findByNameIgnoreCase(variety.asText()).filter(v -> v.isActive()).isEmpty()) {
                throw ApiException.badRequest("A variedade padrão deve corresponder a um tipo de uva ativo.");
            }
        }
        if (section.equals("appearance")) {
            String theme = data.path("theme").asText("");
            if (!theme.equals("light") && !theme.equals("dark")) throw ApiException.badRequest("Tema inválido.");
        }
        if (section.equals("security")) {
            int timeout;
            try { timeout = Integer.parseInt(data.path("sessionTimeout").asText("")); }
            catch (NumberFormatException exception) { throw ApiException.badRequest("Duração de sessão inválida."); }
            if (timeout < 5 || timeout > 1440) throw ApiException.badRequest("Duração de sessão inválida.");
            if (data.path("requireMfa").asBoolean(false)) {
                throw ApiException.badRequest("MFA ainda não está configurado por um provedor de identidade.");
            }
            if (data.path("notifyNewLogin").asBoolean(false)) {
                throw ApiException.badRequest("Notificações por e-mail ainda não estão configuradas.");
            }
        }
    }

    private void validateText(ObjectNode data, String key, int maxLength) {
        JsonNode value = data.get(key);
        if (value == null || !value.isTextual() || value.asText().isBlank() || value.asText().length() > maxLength) {
            throw ApiException.badRequest("O campo de configuração '" + key + "' é obrigatório e deve ter até " + maxLength + " caracteres.");
        }
    }

    private ObjectNode defaults() {
        ObjectNode root = mapper.createObjectNode();
        root.set("general", mapper.createObjectNode()
            .put("systemName", "AgroClima Cloud")
            .put("region", "Petrolina / Juazeiro")
            .put("institutionalInfo", "Projeto Integrador — Análise e Desenvolvimento de Sistemas")
            .put("defaultVariety", "Uva Itália"));
        root.set("notifications", mapper.createObjectNode()
            .put("climateAlerts", true).put("systemAlerts", true).put("emailSummary", false));
        root.set("appearance", mapper.createObjectNode().put("theme", "light"));
        root.set("security", mapper.createObjectNode()
            .put("strongPasswords", true).put("requireMfa", false).put("notifyNewLogin", false)
            .put("lockAfterFailures", true).put("sessionTimeout", Long.toString(properties.security().tokenTtlMinutes())));
        return root;
    }
}
