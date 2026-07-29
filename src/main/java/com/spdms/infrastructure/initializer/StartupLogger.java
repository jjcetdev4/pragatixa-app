package com.pragatix.infrastructure.initializer;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("====================================================");
        System.out.println(" SPDMS Backend Started Successfully");
        System.out.println(" Server : http://localhost:8080");
        System.out.println(" Database : MySQL Connected");
        System.out.println(" Java : 21");
        System.out.println(" Spring Boot : Running");
        System.out.println("====================================================");
    }
}
