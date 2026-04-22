package com.bhishi;

// ============================================================
// DIGITAL BHISHI PLATFORM — Application Entry Point
// File: Foundation.java | Package: com.bhishi
// Note: Exceptions moved to com.bhishi.exception.Exceptions
//       Utilities moved to com.bhishi.util.Utils
// ============================================================

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableMongoAuditing   // enables @CreatedDate / @LastModifiedDate on all models
@EnableScheduling      // enables @Scheduled for monthly cycle automation jobs
public class BhishiPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(BhishiPlatformApplication.class, args);
    }
}
