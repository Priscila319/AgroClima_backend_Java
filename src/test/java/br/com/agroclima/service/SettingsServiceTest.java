package br.com.agroclima.service;

import br.com.agroclima.config.AppProperties;
import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.exception.ApiException;
import br.com.agroclima.repository.AppSettingRepository;
import br.com.agroclima.repository.UserRepository;
import br.com.agroclima.repository.VarietyRepository;
import br.com.agroclima.security.AgroUserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {
    @Mock AppSettingRepository settingsRepository;
    @Mock UserRepository users;
    @Mock VarietyRepository varieties;
    @Spy ObjectMapper mapper = new ObjectMapper();
    @Mock AuditService audit;
    @Mock AppProperties properties;

    SettingsService service;
    AgroUserPrincipal actor;

    @BeforeEach
    void setUp() {
        service = new SettingsService(settingsRepository, users, varieties, mapper, audit, properties);
        actor = new AgroUserPrincipal(UUID.randomUUID(), "Admin", "admin@example.test", "hash",
            UserEntity.Status.ACTIVE, null, Set.of("ADMIN"), Set.of("settings.manage"));
        lenient().when(properties.security()).thenReturn(
            new AppProperties.Security("test-secret-with-more-than-32-characters-123456", 60, "admin@example.test", "", "", 5, 15));
        lenient().when(settingsRepository.findByKey(any())).thenReturn(Optional.empty());
    }

    private ObjectNode validDesign() {
        ObjectNode design = mapper.createObjectNode();
        for (String theme : new String[] {"light", "dark"}) {
            design.set(theme, mapper.createObjectNode()
                .put("primary", "#167c55").put("primaryHover", "#116d4a").put("secondary", "#0b3024")
                .put("background", "#f5f8f6").put("surface", "#ffffff").put("text", "#192923")
                .put("textMuted", "#6d7d76").put("border", "#e2e9e5")
                .put("button", "#167c55").put("buttonText", "#ffffff"));
        }
        design.set("typography", mapper.createObjectNode().put("bodyFont", "dm-sans").put("headingFont", "manrope").put("fontScale", 100));
        design.set("components", mapper.createObjectNode().put("cardRadius", 15).put("buttonRadius", 9).put("borderWidth", 1).put("cardShadow", "soft"));
        return design;
    }

    private void allowSave() {
        when(users.getReferenceById(actor.id())).thenReturn(
            new UserEntity("Admin", "admin@example.test", "hash", "", "", "", Set.of()));
        when(settingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void defaultsIncludeDesignTokensForBothThemes() {
        JsonNode result = service.get();
        assertEquals("#167c55", result.path("design").path("light").path("primary").asText());
        assertEquals("#5fc28d", result.path("design").path("dark").path("primary").asText());
        assertEquals(100, result.path("design").path("typography").path("fontScale").asInt());
    }

    @Test
    void updatePersistsValidDesignCustomization() {
        allowSave();
        ObjectNode design = validDesign();
        ((ObjectNode) design.get("light")).put("primary", "#7c3d67");
        ((ObjectNode) design.get("typography")).put("fontScale", 110);

        JsonNode result = service.update(mapper.createObjectNode().set("design", design), actor);

        assertEquals("#7c3d67", result.path("design").path("light").path("primary").asText());
        assertEquals(110, result.path("design").path("typography").path("fontScale").asInt());
        verify(settingsRepository).save(any());
        verify(audit).record(actor, "Atualizou configurações", "Configurações", "Preferências do sistema atualizadas.");
    }

    @Test
    void updateRejectsInvalidHexColor() {
        ObjectNode design = validDesign();
        ((ObjectNode) design.get("dark")).put("surface", "red; background: url(x)");

        ApiException error = assertThrows(ApiException.class,
            () -> service.update(mapper.createObjectNode().set("design", design), actor));
        assertEquals(400, error.getStatus().value());
        assertTrue(error.getMessage().contains("dark.surface"));
    }

    @Test
    void updateRejectsUnknownFont() {
        ObjectNode design = validDesign();
        ((ObjectNode) design.get("typography")).put("bodyFont", "comic-external");

        assertThrows(ApiException.class, () -> service.update(mapper.createObjectNode().set("design", design), actor));
    }

    @Test
    void updateRejectsFontScaleOutOfRange() {
        ObjectNode design = validDesign();
        ((ObjectNode) design.get("typography")).put("fontScale", 400);

        assertThrows(ApiException.class, () -> service.update(mapper.createObjectNode().set("design", design), actor));
    }

    @Test
    void updateRejectsUnknownCardShadow() {
        ObjectNode design = validDesign();
        ((ObjectNode) design.get("components")).put("cardShadow", "glow");

        assertThrows(ApiException.class, () -> service.update(mapper.createObjectNode().set("design", design), actor));
    }

    @Test
    void publicViewExposesDesignButNotSecurityOrNotifications() {
        JsonNode result = service.publicView();
        assertTrue(result.has("design"));
        assertTrue(result.has("appearance"));
        assertTrue(result.has("general"));
        assertFalse(result.has("security"));
        assertFalse(result.has("notifications"));
    }
}
