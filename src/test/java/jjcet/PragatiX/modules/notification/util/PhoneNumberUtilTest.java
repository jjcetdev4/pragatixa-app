package jjcet.PragatiX.modules.notification.util;

import jjcet.PragatiX.modules.notification.exception.NotificationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberUtilTest {

    @Test
    void testNormalizeIndianPhoneNumber_10Digits() {
        assertEquals("919591234567", PhoneNumberUtil.normalizeIndianPhoneNumber("9591234567"));
        assertEquals("918881234567", PhoneNumberUtil.normalizeIndianPhoneNumber("8881234567"));
        assertEquals("917012345678", PhoneNumberUtil.normalizeIndianPhoneNumber("7012345678"));
        assertEquals("916234567890", PhoneNumberUtil.normalizeIndianPhoneNumber("6234567890"));
    }

    @Test
    void testNormalizeIndianPhoneNumber_WithPlus91() {
        assertEquals("919591234567", PhoneNumberUtil.normalizeIndianPhoneNumber("+919591234567"));
        assertEquals("919876543210", PhoneNumberUtil.normalizeIndianPhoneNumber("+91 9876543210"));
    }

    @Test
    void testNormalizeIndianPhoneNumber_Already91() {
        assertEquals("919591234567", PhoneNumberUtil.normalizeIndianPhoneNumber("919591234567"));
    }

    @Test
    void testNormalizeIndianPhoneNumber_WithHyphensAndSpaces() {
        assertEquals("919591234567", PhoneNumberUtil.normalizeIndianPhoneNumber("+91-959-123-4567"));
        assertEquals("919591234567", PhoneNumberUtil.normalizeIndianPhoneNumber("(+91) 959 123 4567"));
    }

    @Test
    void testNormalizeIndianPhoneNumber_WithLeadingZero() {
        assertEquals("919591234567", PhoneNumberUtil.normalizeIndianPhoneNumber("09591234567"));
    }

    @Test
    void testNormalizeIndianPhoneNumber_InvalidNumber() {
        assertThrows(NotificationException.class, () -> PhoneNumberUtil.normalizeIndianPhoneNumber(null));
        assertThrows(NotificationException.class, () -> PhoneNumberUtil.normalizeIndianPhoneNumber(""));
        assertThrows(NotificationException.class, () -> PhoneNumberUtil.normalizeIndianPhoneNumber("   "));
        assertThrows(NotificationException.class, () -> PhoneNumberUtil.normalizeIndianPhoneNumber("12345"));
        assertThrows(NotificationException.class, () -> PhoneNumberUtil.normalizeIndianPhoneNumber("abcdefghij"));
    }

    @Test
    void testMaskPhoneNumber() {
        assertEquals("******4567", PhoneNumberUtil.maskPhoneNumber("919591234567"));
        assertEquals("******3210", PhoneNumberUtil.maskPhoneNumber("+919876543210"));
        assertEquals("******", PhoneNumberUtil.maskPhoneNumber("123"));
        assertEquals("******", PhoneNumberUtil.maskPhoneNumber(null));
    }
}
