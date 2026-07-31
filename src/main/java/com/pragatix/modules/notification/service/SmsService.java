package com.pragatix.modules.notification.service;

public interface SmsService {
    String sendSms(String phone, String message);
}
