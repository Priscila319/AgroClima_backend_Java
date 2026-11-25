package br.com.agroclima.domain;

import br.com.agroclima.security.EncryptedStringConverter;
import jakarta.persistence.Convert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Garante que o telefone do usuário continua sendo persistido pelo conversor de criptografia. */
class UserEntityEncryptionTest {
    @Test
    void phoneColumnUsesTheEncryptedStringConverter() throws NoSuchFieldException {
        Convert convert = UserEntity.class.getDeclaredField("phone").getAnnotation(Convert.class);

        assertNotNull(convert, "o campo phone precisa de @Convert");
        assertEquals(EncryptedStringConverter.class, convert.converter());
    }
}
