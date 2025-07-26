package com.flightmanagement.flight.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class FlightServiceProperties {

    private Jwt jwt = new Jwt();
    private Kafka kafka = new Kafka();
    private Redis redis = new Redis();
    private FileUpload fileUpload = new FileUpload();
    private ReferenceManager referenceManager = new ReferenceManager();

    @Data
    public static class Jwt {
        private String secret = "flightManagementSecretKey123456789";
        private long expiration = 86400000; // 24 hours
    }

    @Data
    public static class Kafka {
        private Topics topics = new Topics();

        @Data
        public static class Topics {
            private String flightEvents = "flight.events";
            private String uploadEvents = "flight.upload.events";
        }
    }

    @Data
    public static class Redis {
        private Ttl ttl = new Ttl();

        @Data
        public static class Ttl {
            private Duration airlines = Duration.ofHours(2);
            private Duration stations = Duration.ofHours(4);
            private Duration aircraft = Duration.ofHours(1);
            private Duration flightLookups = Duration.ofMinutes(30);
        }
    }

    @Data
    public static class FileUpload {
        private long maxSize = 10485760; // 10MB
        private int maxRows = 10000;
        private String[] allowedTypes = {"text/csv", "application/csv"};
    }

    @Data
    public static class ReferenceManager {
        private String baseUrl = "http://localhost:8081/reference-manager";
        private Duration timeout = Duration.ofSeconds(5);
        private int retryAttempts = 3;
        private Duration retryDelay = Duration.ofSeconds(1);
    }
}