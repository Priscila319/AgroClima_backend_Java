package br.com.agroclima.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {
    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> PasswordPolicy.require("Agroclima2026", true));
    }

    @Test
    void rejectsPasswordWithoutNumberWhenStrongPolicyIsEnabled() {
        assertThrows(RuntimeException.class, () -> PasswordPolicy.require("AgroclimaABC", true));
    }

    @Test
    void alwaysEnforcesMinimumLength() {
        assertThrows(RuntimeException.class, () -> PasswordPolicy.require("abc123", false));
    }
}
