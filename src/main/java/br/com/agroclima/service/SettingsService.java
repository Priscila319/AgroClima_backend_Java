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
    private static final Set<String> SECTIONS = Set.of("general", "notifications", "appearance", "security", "design");
    private static final Set<String> DESIGN_THEMES = Set.of("light", "dark");
    private static final Set<String> DESIGN_COLOR_KEYS = Set.of(
        "primary", "primaryHover", "secondary", "background", "surface", "text", "textMuted", "border", "button", "buttonText");
    private static final Set<String> DESIGN_FONTS = Set.of("dm-sans", "manrope", "system", "serif", "mono");
    private static final Set<String> DESIGN_SHADOWS = Set.of("none", "soft", "medium", "strong");
    private static final java.util.regex.Pattern HEX_COLOR = java.util.regex.Pattern.compile("^#[0-9a-fA-F]{6}$");
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
        if (section.equals("design")) {
            validateDesign(data);
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

    /**
     * Valida os tokens do Design System: cores hex por tema, fontes do catálogo,
     * escala tipográfica e raios/sombra de componentes. Os limites mantêm a
     * responsividade e a legibilidade do layout existente.
     */
    private void validateDesign(ObjectNode data) {
        for (String theme : DESIGN_THEMES) {
            JsonNode palette = data.get(theme);
            if (palette == null || !palette.isObject()) throw ApiException.badRequest("A paleta do tema '" + theme + "' é obrigatória.");
            for (String key : DESIGN_COLOR_KEYS) {
                JsonNode color = palette.get(key);
                if (color == null || !color.isTextual() || !HEX_COLOR.matcher(color.asText()).matches()) {
                    throw ApiException.badRequest("Cor inválida em '" + theme + "." + key + "': use o formato hexadecimal #RRGGBB.");
                }
            }
        }
        JsonNode typography = data.get("typography");
        if (typography == null || !typography.isObject()) throw ApiException.badRequest("A seção de tipografia é obrigatória.");
        for (String key : Set.of("bodyFont", "headingFont")) {
            JsonNode font = typography.get(key);
            if (font == null || !font.isTextual() || !DESIGN_FONTS.contains(font.asText())) {
                throw ApiException.badRequest("Fonte inválida em 'typography." + key + "'.");
            }
        }
        validateDesignRange(typography, "fontScale", 85, 125);
        JsonNode components = data.get("components");
        if (components == null || !components.isObject()) throw ApiException.badRequest("A seção de componentes é obrigatória.");
        validateDesignRange(components, "cardRadius", 0, 28);
        validateDesignRange(components, "buttonRadius", 0, 24);
        validateDesignRange(components, "borderWidth", 0, 3);
        JsonNode shadow = components.get("cardShadow");
        if (shadow == null || !shadow.isTextual() || !DESIGN_SHADOWS.contains(shadow.asText())) {
            throw ApiException.badRequest("Sombra de card inválida: use none, soft, medium ou strong.");
        }
    }

    // CORREÇÃO: o parâmetro agora é JsonNode (antes era ObjectNode), pois
    // data.get(...) retorna JsonNode e o método só usa get(...).
    private void validateDesignRange(JsonNode data, String key, int min, int max) {
        JsonNode value = data.get(key);
        if (value == null || !value.isNumber() || value.asInt() < min || value.asInt() > max) {
            throw ApiException.badRequest("O valor de '" + key + "' deve ser um número entre " + min + " e " + max + ".");
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
        root.set("design", designDefaults());
        return root;
    }

    /**
     * Tokens padrão do Design System. Os valores espelham exatamente os tokens
     * atuais de css/variables.css e css/themes.css, para que restaurar o padrão
     * preserve a identidade visual original em ambos os temas.
     */
    private ObjectNode designDefaults() {
        ObjectNode design = mapper.createObjectNode();
        design.set("light", mapper.createObjectNode()
            .put("primary", "#167c55").put("primaryHover", "#116d4a").put("secondary", "#0b3024")
            .put("background", "#f5f8f6").put("surface", "#ffffff").put("text", "#192923")
            .put("textMuted", "#6d7d76").put("border", "#e2e9e5")
            .put("button", "#167c55").put("buttonText", "#ffffff"));
        design.set("dark", mapper.createObjectNode()
            .put("primary", "#5fc28d").put("primaryHover", "#116d4a").put("secondary", "#d4e9dc")
            .put("background", "#101a15").put("surface", "#1a2922").put("text", "#e7f0ea")
            .put("textMuted", "#a8b9b0").put("border", "#34483d")
            .put("button", "#5fc28d").put("buttonText", "#ffffff"));
        design.set("typography", mapper.createObjectNode()
            .put("bodyFont", "dm-sans").put("headingFont", "manrope").put("fontScale", 100));
        design.set("components", mapper.createObjectNode()
            .put("cardRadius", 15).put("buttonRadius", 9).put("borderWidth", 1).put("cardShadow", "soft"));
        return design;
    }

    /**
     * Visão pública (qualquer usuário autenticado): identidade do sistema, tema e
     * tokens de design. Não expõe notificações nem políticas de segurança.
     */
    @Transactional(readOnly = true)
    public JsonNode publicView() {
        JsonNode all = get();
        ObjectNode general = (ObjectNode) all.get("general").deepCopy();
        ObjectNode result = mapper.createObjectNode();
        result.set("general", general);
        result.set("appearance", all.get("appearance").deepCopy());
        result.set("design", all.get("design").deepCopy());
        return result;
    }
}