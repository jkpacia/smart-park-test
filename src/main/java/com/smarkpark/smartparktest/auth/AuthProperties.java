package com.smarkpark.smartparktest.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties("smartpark.auth")
@Validated
public class AuthProperties {
    @NotBlank
    private String username, password, secret, issuer;
    @NotNull
    private Duration tokenTtl;

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        username = v;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String v) {
        password = v;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String v) {
        secret = v;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String v) {
        issuer = v;
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration v) {
        tokenTtl = v;
    }

    @AssertTrue(message = "JWT secret must contain at least 32 UTF-8 bytes")
    public boolean isSecretLongEnough() {
        return secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= 32;
    }

    @AssertTrue(message = "Token lifetime must be positive")
    public boolean isTokenTtlPositive() {
        return tokenTtl != null && !tokenTtl.isZero() && !tokenTtl.isNegative();
    }
}

