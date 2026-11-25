package br.com.agroclima.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptedStringConverterTest {
    private static final String KEY_A = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
    private static final String KEY_B = Base64.getEncoder().encodeToString("fedcba9876543210fedcba9876543210".getBytes());
    private static final String PHONE = "5581987654321";

    private final EncryptedStringConverter converter = new EncryptedStringConverter(KEY_A);

    @Test
    void encryptsAndDecryptsBackToTheOriginalValue() {
        String stored = converter.convertToDatabaseColumn(PHONE);

        assertTrue(stored.startsWith("enc:v1:"));
        assertFalse(stored.contains(PHONE), "o valor em claro não pode aparecer no texto cifrado");
        assertEquals(PHONE, converter.convertToEntityAttribute(stored));
    }

    @Test
    void sameValueProducesDifferentCiphertextBecauseTheIvIsRandom() {
        assertNotEquals(converter.convertToDatabaseColumn(PHONE), converter.convertToDatabaseColumn(PHONE));
    }

    @Test
    void storedValueFitsTheWidenedColumn() {
        String stored = converter.convertToDatabaseColumn("+55 (81) 98765-4321 ramal 1234567");
        assertTrue(stored.length() <= 255);
    }

    @Test
    void nullAndEmptyValuesAreLeftAlone() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertEquals("", converter.convertToDatabaseColumn(""));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void legacyPlaintextRowsStayReadable() {
        assertEquals(PHONE, converter.convertToEntityAttribute(PHONE));
        assertEquals("", converter.convertToEntityAttribute(""));
    }

    @Test
    void handlesNonAsciiText() {
        String text = "Fazenda São João — ramal 12";
        assertEquals(text, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(text)));
    }

    @Test
    void tamperedCiphertextIsRejectedInsteadOfReturningGarbage() {
        String stored = converter.convertToDatabaseColumn(PHONE);
        char last = stored.charAt(stored.length() - 5);
        String tampered = stored.substring(0, stored.length() - 5) + (last == 'A' ? 'B' : 'A') + stored.substring(stored.length() - 4);

        assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute(tampered));
    }

    @Test
    void wrongKeyCannotDecrypt() {
        String stored = converter.convertToDatabaseColumn(PHONE);
        EncryptedStringConverter other = new EncryptedStringConverter(KEY_B);

        assertThrows(IllegalStateException.class, () -> other.convertToEntityAttribute(stored));
    }

    @Test
    void truncatedPayloadIsRejected() {
        assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute("enc:v1:AAAA"));
    }

    @Test
    void missingOrInvalidKeyFailsFast() {
        assertThrows(IllegalStateException.class, () -> new EncryptedStringConverter(""));
        assertThrows(IllegalStateException.class, () -> new EncryptedStringConverter(null));
        assertThrows(IllegalStateException.class, () -> new EncryptedStringConverter("não-é-base64!!"));
        String shortKey = Base64.getEncoder().encodeToString("curta".getBytes());
        assertThrows(IllegalStateException.class, () -> new EncryptedStringConverter(shortKey));
    }
}
