package com.smarkpark.smartparktest.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.smarkpark.smartparktest.common.JsonErrors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfiguration {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService users(AuthProperties p, PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(User.withUsername(p.getUsername()).password(encoder.encode(p.getPassword())).roles("USER").build());
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecretKey signingKey(AuthProperties p) {
        return new SecretKeySpec(p.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey key, AuthProperties p, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamp = new JwtTimestampValidator(Duration.ZERO);
        timestamp.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp, new JwtIssuerValidator(p.getIssuer()),
                new JwtClaimValidator<Instant>("exp", exp -> exp != null && clock.instant().isBefore(exp)),
                new JwtClaimValidator<String>("sub", sub -> sub != null && !sub.isBlank())));
        return decoder;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, JsonErrors errors) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> errors.write(req, res, 401, "UNAUTHORIZED", "A valid bearer token is required"))
                        .accessDeniedHandler((req, res, e) -> errors.write(req, res, 403, "FORBIDDEN", "Access denied")))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {
                        })
                        .authenticationEntryPoint((req, res, e) -> errors.write(req, res, 401, "UNAUTHORIZED", "A valid bearer token is required")));
        return http.build();
    }
}


