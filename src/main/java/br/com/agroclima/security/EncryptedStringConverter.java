package br.com.agroclima.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografia de campo em repouso (AES-256-GCM) para dados pessoais guardados no MySQL.
 *
 * <p>Formato armazenado: {@code enc:v1:} + Base64(IV de 12 bytes || texto cifrado || tag GCM de 16 bytes).
 * Cada valor usa um IV aleatório, então o mesmo telefone gera textos cifrados diferentes. O GCM também
 * autentica o conteúdo: um valor adulterado no banco falha na leitura em vez de voltar corrompido.
 *
 * <p>Valores sem o prefixo (linhas antigas, gravadas em claro) são devolvidos como estão e passam a ser
 * cifrados na próxima vez que o registro for salvo. A chave vem de {@code FIELD_ENCRYPTION_KEY}
 * (Base64 de 32 bytes) e nunca do código-fonte; perdê-la significa perder os dados cifrados.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    static final String PREFIX = "enc:v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public EncryptedStringConverter(@Value("${app.security.field-encryption-key:}") String base64Key) {
        this.key = parseKey(base64Key);
    }

    static SecretKeySpec parseKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                "FIELD_ENCRYPTION_KEY não definida. Gere uma com: openssl rand -base64 32");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("FIELD_ENCRYPTION_KEY deve estar em Base64.", e);
        }
        if (raw.length != 32) {
            throw new IllegalStateException("FIELD_ENCRYPTION_KEY deve ter exatamente 32 bytes (AES-256) em Base64.");
        }
        return new SecretKeySpec(raw, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String plain) {
        if (plain == null || plain.isEmpty()) return plain;
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(IV_BYTES + encrypted.length).put(iv).put(encrypted).array();
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar o campo.", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) return stored;
        try {
            byte[] payload = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (payload.length <= IV_BYTES) throw new IllegalStateException("Valor cifrado inválido.");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, payload, 0, IV_BYTES));
            return new String(cipher.doFinal(payload, IV_BYTES, payload.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Falha ao decifrar o campo (chave incorreta ou valor adulterado).", e);
        }
    }
}
