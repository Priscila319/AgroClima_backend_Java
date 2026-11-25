package br.com.agroclima.service;

import br.com.agroclima.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumberTest {

    @Test
    void acceptsValidMobileWithMaskAndStoresDigitsWithCountryCode() {
        assertEquals("5581999998888", PhoneNumber.normalize("(81) 99999-8888"));
    }

    @Test
    void acceptsValidLandlineWithMask() {
        assertEquals("558133334444", PhoneNumber.normalize("(81) 3333-4444"));
    }

    @Test
    void acceptsValueAlreadyStoredWithCountryCode() {
        assertEquals("5581999998888", PhoneNumber.normalize("5581999998888"));
        assertEquals("558133334444", PhoneNumber.normalize("+55 (81) 3333-4444"));
    }

    @Test
    void keepsEmptyOptionalPhone() {
        assertEquals("", PhoneNumber.normalize(null));
        assertEquals("", PhoneNumber.normalize("   "));
        assertEquals("", PhoneNumber.normalize("() -"));
    }

    @Test
    void stripsLettersAndSpecialCharactersBeforeValidating() {
        assertEquals("5581999998888", PhoneNumber.normalize("abc(81) 99999-8888xyz"));
    }

    @Test
    void rejectsIncompleteNumber() {
        assertEquals(400, assertThrows(ApiException.class, () -> PhoneNumber.normalize("(81) 9999-888")).getStatus().value());
    }

    @Test
    void rejectsMoreThanOneNumberInSameField() {
        assertEquals(400, assertThrows(ApiException.class,
            () -> PhoneNumber.normalize("(81) 99999-8888 (81) 98888-7777")).getStatus().value());
    }

    @Test
    void rejectsInvalidDdd() {
        assertEquals(400, assertThrows(ApiException.class, () -> PhoneNumber.normalize("(00) 99999-8888")).getStatus().value());
        assertEquals(400, assertThrows(ApiException.class, () -> PhoneNumber.normalize("(23) 99999-8888")).getStatus().value());
    }

    @Test
    void rejectsRepeatedDigitSequences() {
        assertEquals(400, assertThrows(ApiException.class, () -> PhoneNumber.normalize("00000000000")).getStatus().value());
        assertEquals(400, assertThrows(ApiException.class, () -> PhoneNumber.normalize("(11) 11111-1111")).getStatus().value());
        assertEquals(400, assertThrows(ApiException.class, () -> PhoneNumber.normalize("(81) 99999-9999")).getStatus().value());
    }

    @Test
    void rejectsMobileWithoutLeadingNine() {
        assertEquals(400, assertThrows(ApiException.class, () -> PhoneNumber.normalize("(81) 89999-8888")).getStatus().value());
    }

    @Test
    void rejectsLandlineWithInvalidFirstDigit() {
        assertEquals(400, assertThrows(ApiException.class, () -> PhoneNumber.normalize("(81) 1333-4444")).getStatus().value());
        assertEquals(400, assertThrows(ApiException.class, () -> PhoneNumber.normalize("(81) 9333-4444")).getStatus().value());
    }
}
