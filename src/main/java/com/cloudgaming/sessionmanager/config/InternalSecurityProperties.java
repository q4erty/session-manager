package com.cloudgaming.sessionmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record InternalSecurityProperties(String internalSecret) {

    public InternalSecurityProperties {
        if (internalSecret == null || internalSecret.isBlank()) {
            throw new IllegalStateException(
                    "app.security.internal-secret must not be blank; set INTERNAL_SECRET");
        }
    }
}
