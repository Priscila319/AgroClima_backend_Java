package br.com.agroclima.service;

import br.com.agroclima.exception.ApiException;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class PasswordPolicy {
    private static final Pattern LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");

    private PasswordPolicy() {}

    public static boolean isStrong(String password) {
        return validLength(password) && LETTER.matcher(password).matches() && DIGIT.matcher(password).matches();
    }

    public static void require(String password, boolean strongPasswords) {
        if (!validLength(password)) {
            throw ApiException.badRequest(strongPasswords
                ? "A senha deve ter entre 8 e 72 bytes e conter ao menos uma letra e um número."
                : "A senha deve ter entre 8 e 72 bytes.");
        }
        if (strongPasswords && (!LETTER.matcher(password).matches() || !DIGIT.matcher(password).matches())) {
            throw ApiException.badRequest("A senha deve ter entre 8 e 72 bytes e conter ao menos uma letra e um número.");
        }
    }

    private static boolean validLength(String password) {
        return password != null && password.length() >= 8
            && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
