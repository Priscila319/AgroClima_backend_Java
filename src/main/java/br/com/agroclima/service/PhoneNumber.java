package br.com.agroclima.service;

import br.com.agroclima.exception.ApiException;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validação e normalização de telefones brasileiros (celular e fixo).
 *
 * Regras aplicadas aqui espelham as do frontend (js/utils/phone.js):
 * - somente dígitos são considerados (máscara/parênteses/traços são removidos);
 * - DDI +55 é opcional na entrada e sempre presente no valor armazenado;
 * - 10 dígitos (fixo, DDD + 8) ou 11 dígitos (celular, DDD + 9);
 * - DDD precisa existir na numeração brasileira;
 * - celular começa com 9; fixo começa com 2, 3, 4 ou 5;
 * - sequências repetidas (ex.: 11111111111) são rejeitadas.
 *
 * O valor persistido contém apenas dígitos, com DDI: "5581999999999".
 * O campo é opcional: vazio/nulo continua aceito e é armazenado como "".
 */
public final class PhoneNumber {
    private static final Pattern NON_DIGITS = Pattern.compile("\\D");
    private static final Pattern REPEATED = Pattern.compile("^(\\d)\\1+$");
    private static final Set<String> VALID_DDD = Set.of(
        "11", "12", "13", "14", "15", "16", "17", "18", "19",
        "21", "22", "24", "27", "28",
        "31", "32", "33", "34", "35", "37", "38",
        "41", "42", "43", "44", "45", "46", "47", "48", "49",
        "51", "53", "54", "55",
        "61", "62", "63", "64", "65", "66", "67", "68", "69",
        "71", "73", "74", "75", "77", "79",
        "81", "82", "83", "84", "85", "86", "87", "88", "89",
        "91", "92", "93", "94", "95", "96", "97", "98", "99"
    );

    private PhoneNumber() {}

    /** Remove tudo que não é dígito. */
    public static String digits(String value) {
        return value == null ? "" : NON_DIGITS.matcher(value).replaceAll("");
    }

    /**
     * Normaliza para somente dígitos com DDI 55 (12 ou 13 caracteres) ou "" quando vazio.
     * Lança {@link ApiException} 400 com mensagem amigável quando o número é inválido.
     */
    public static String normalize(String value) {
        String digits = digits(value);
        if (digits.isEmpty()) return "";
        if (digits.startsWith("55") && (digits.length() == 12 || digits.length() == 13)) {
            digits = digits.substring(2);
        }
        if (digits.length() < 10) {
            throw ApiException.badRequest("Telefone incompleto: informe DDD + número (10 dígitos para fixo ou 11 para celular).");
        }
        if (digits.length() > 11) {
            throw ApiException.badRequest("Informe apenas um número de telefone com DDD (10 ou 11 dígitos).");
        }
        String ddd = digits.substring(0, 2);
        if (!VALID_DDD.contains(ddd)) {
            throw ApiException.badRequest("DDD inválido: use um código de área brasileiro existente, ex.: (81).");
        }
        String number = digits.substring(2);
        if (REPEATED.matcher(number).matches() || REPEATED.matcher(digits).matches()) {
            throw ApiException.badRequest("Número de telefone inválido: não use sequências repetidas.");
        }
        if (digits.length() == 11 && number.charAt(0) != '9') {
            throw ApiException.badRequest("Celular com 9 dígitos deve começar com 9, ex.: (81) 99999-9999.");
        }
        if (digits.length() == 10 && (number.charAt(0) < '2' || number.charAt(0) > '5')) {
            throw ApiException.badRequest("Telefone fixo deve começar com 2, 3, 4 ou 5, ex.: (81) 3333-3333.");
        }
        return "55" + digits;
    }
}
