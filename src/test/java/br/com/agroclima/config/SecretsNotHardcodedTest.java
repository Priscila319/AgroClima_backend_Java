package br.com.agroclima.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Teste de segurança "secret pesquisado no código": nenhum segredo literal no application.yml. */
class SecretsNotHardcodedTest {
    private static final List<String> SECRET_KEYS = List.of(
        "jwt-secret", "initial-admin-password", "internal-api-key", "field-encryption-key",
        "read-api-key", "password");

    @Test
    void secretKeysInApplicationYamlOnlyReferenceEnvironmentVariables() throws IOException {
        Path yaml = Path.of("src/main/resources/application.yml");
        assertTrue(Files.exists(yaml), "execute os testes a partir da pasta backend-java");
        List<String> lines = Files.readAllLines(yaml);

        for (String key : SECRET_KEYS) {
            Pattern pattern = Pattern.compile("^\\s*" + Pattern.quote(key) + ":\\s*(.*)$");
            int found = 0;
            for (String line : lines) {
                Matcher m = pattern.matcher(line);
                if (!m.matches()) continue;
                found++;
                String value = m.group(1).trim();
                assertTrue(value.startsWith("${") && value.endsWith("}"),
                    key + " deve vir de variável de ambiente, mas está como: " + value);
            }
            assertTrue(found > 0, "chave não encontrada no yml: " + key);
        }
    }

    @Test
    void datasourceCredentialsComeFromTheEnvironment() throws IOException {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"));
        assertTrue(yaml.contains("username: ${DATABASE_USER}"));
        assertEquals(-1, yaml.indexOf("password: root"));
        assertEquals(-1, yaml.indexOf("admin123"));
    }
}
