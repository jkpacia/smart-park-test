package com.smarkpark.smartparktest.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager manager;
    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final Clock clock;

    public AuthController(AuthenticationManager manager, JwtEncoder encoder, AuthProperties properties, Clock clock) {
        this.manager = manager;
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication user = manager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.getIssuer()).subject(user.getName())
                .issuedAt(now).expiresAt(now.plus(properties.getTokenTtl())).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", properties.getTokenTtl().getSeconds());
    }

    public static class LoginRequest {
        @NotBlank
        private String username, password;

        public LoginRequest() {
        }

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
    }

    public static class TokenResponse {
        private final String accessToken, tokenType;
        private final long expiresIn;

        public TokenResponse(String accessToken, String tokenType, long expiresIn) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public long getExpiresIn() {
            return expiresIn;
        }
    }
}


