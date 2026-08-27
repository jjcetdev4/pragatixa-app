package jjcet.PragatiX.modules.notification.service;

public interface SmsService {

    /**
     * Sends an SMS message to the given destination phone number.
     *
     * @param phone Destination phone number
     * @param message Text message body
     * @return Provider message reference / ID
     */
    String sendSms(String phone, String message);

    /**
     * Sends an SMS message with a specified purpose (e.g. OTP, ABSENCE, NOTIFICATION).
     *
     * @param phone Destination phone number
     * @param message Text message body
     * @param purpose Categorization purpose for DLT template mapping and logs
     * @return Provider message reference / ID
     */
    default String sendSms(String phone, String message, String purpose) {
        return sendSms(phone, message);
    }
}
