package br.com.agroclima.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThingSpeakPropertiesTest {

    private static ThingSpeakProperties props(boolean enabled, String channel, String fieldMap) {
        return new ThingSpeakProperties(enabled, channel, "read-key", "https://api.thingspeak.com", fieldMap, 300, 15, 500);
    }

    @Test
    void blankFieldMapUsesOfficialTemperatureAndHumidityMapping() {
        ThingSpeakProperties properties = props(true, "3499301", "");

        assertEquals("field1", properties.fieldMapping().get("temperature"));
        assertEquals("field2", properties.fieldMapping().get("humidity"));
        assertEquals(2, properties.fieldMapping().size());
    }

    @Test
    void rejectsAnyFieldMapOtherThanTemperatureField1AndHumidityField2() {
        assertThrows(IllegalStateException.class,
            () -> props(true, "3499301", "{\"temperature\":\"field3\",\"humidity\":\"field2\"}"));
        assertThrows(IllegalStateException.class,
            () -> props(true, "3499301", "{\"temperature\":\"field1\"}"));
    }

    @Test
    void enabledIntegrationRequiresNumericChannelId() {
        assertThrows(IllegalStateException.class, () -> props(true, "", ""));
        assertThrows(IllegalStateException.class, () -> props(true, "canal-abc", ""));
    }

    @Test
    void disabledIntegrationAcceptsMissingChannel() {
        ThingSpeakProperties properties = props(false, "", "");

        assertFalse(properties.configured());
    }
}
