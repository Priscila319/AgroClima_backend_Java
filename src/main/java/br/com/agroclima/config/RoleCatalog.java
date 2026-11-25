package br.com.agroclima.config;

import java.util.Map;

public final class RoleCatalog {
    public static final String PRODUCER = "PRODUCER";
    public static final String ANALYST = "ANALYST";
    public static final String ADMIN = "ADMIN";

    private static final Map<String, String> LABELS = Map.of(
        PRODUCER, "Produtor/Exportador",
        ANALYST, "Analista de Dados",
        ADMIN, "Administrador"
    );

    private RoleCatalog() {}

    public static String label(String key) { return LABELS.getOrDefault(key, key); }
    public static boolean contains(String key) { return LABELS.containsKey(key); }
}
