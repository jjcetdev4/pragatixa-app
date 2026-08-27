package jjcet.PragatiX.modules.notification.util;

import jjcet.PragatiX.modules.notification.exception.NotificationException;

public final class PhoneNumberUtil {

    private PhoneNumberUtil() {
        // Utility class
    }

    /**
     * Normalizes a phone number for Indian SMS gateways (Airtel IQ expects 91XXXXXXXXXX format).
     *
     * Rules:
     * - Cleans out spaces, hyphens, brackets, and '+' symbols.
     * - If 10-digit number (e.g., 9591234567), prepends "91" -> 919591234567.
     * - If already prefixed with "91" (12 digits), retains it -> 919591234567.
     * - If prefixed with "0" (11 digits), strips leading zero and prepends "91".
     * - If international number (non-Indian with country code), retains standard digits.
     *
     * @param phone The raw phone number input
     * @return Normalized phone number string
     */
    public static String normalizeIndianPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new NotificationException("Destination phone number cannot be null or empty.");
        }

        // Strip non-digit characters except leading plus if any
        String cleaned = phone.trim().replaceAll("[\\s\\-\\(\\)\\.]", "");

        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }

        // Handle 0-prefixed 11-digit numbers (e.g., 09591234567 -> 9591234567 -> 919591234567)
        if (cleaned.startsWith("0") && cleaned.length() == 11) {
            cleaned = cleaned.substring(1);
        }

        // If standard 10-digit Indian mobile number
        if (cleaned.length() == 10 && cleaned.matches("^[6-9]\\d{9}$")) {
            return "91" + cleaned;
        }

        // If 12-digit Indian number starting with 91
        if (cleaned.length() == 12 && cleaned.startsWith("91") && cleaned.substring(2).matches("^[6-9]\\d{9}$")) {
            return cleaned;
        }

        // Fallback for general valid digits between 10 and 15 digits
        if (cleaned.matches("^\\d{10,15}$")) {
            if (cleaned.length() == 10) {
                return "91" + cleaned;
            }
            return cleaned;
        }

        throw new NotificationException("Invalid phone number format: " + maskPhoneNumber(phone));
    }

    /**
     * Safely masks a phone number for logging, e.g. "******6460".
     *
     * @param phone Raw or normalized phone number
     * @return Masked string
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "******";
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 4) {
            return "******";
        }
        String lastFour = trimmed.substring(trimmed.length() - 4);
        return "******" + lastFour;
    }
}
