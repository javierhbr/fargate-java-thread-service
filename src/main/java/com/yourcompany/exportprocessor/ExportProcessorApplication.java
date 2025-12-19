package com.yourcompany.exportprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExportProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExportProcessorApplication.class, args);
    }
}
