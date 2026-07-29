package com.spdms.modules.notification.service;

public interface SmsService {
    String sendSms(String phone, String message);
}
