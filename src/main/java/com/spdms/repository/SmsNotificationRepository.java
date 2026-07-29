package com.pragatix.repository;

import com.pragatix.entity.SmsNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmsNotificationRepository extends JpaRepository<SmsNotification, Long> {
}
