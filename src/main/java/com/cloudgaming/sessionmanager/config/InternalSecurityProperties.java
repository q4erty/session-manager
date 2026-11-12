package com.cloudgaming.sessionmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record InternalSecurityProperties(String internalSecret) {
}
