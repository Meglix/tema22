package com.example.demo.config;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.text.ParseException;

@Configuration
public class JwtConfig {

    @FunctionalInterface
    public interface JwtTokenParser {
        SignedJWT parse(String tokenString);
    }

    @Bean
    public JwtTokenParser jwtTokenParser() {
        return tokenString -> {
            try {
                return SignedJWT.parse(tokenString);
            } catch (ParseException e) {
                throw new IllegalStateException("An already validated token was malformed. Check gateway/proxy configuration.", e);
            }
        };
    }
}
