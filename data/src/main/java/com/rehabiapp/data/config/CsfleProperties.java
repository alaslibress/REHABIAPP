package com.rehabiapp.data.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rehabiapp.csfle")
public record CsfleProperties(
        boolean enabled,
        String keyVaultNamespace,
        String kmsProvider,
        String masterKeyPath
) {}
