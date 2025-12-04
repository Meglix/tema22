package com.example.demo.services;


import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.example.demo.config.JwtConfig.JwtTokenParser;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Service
public class JwtService {

    private final JwtTokenParser tokenParser;

    public JwtService(JwtTokenParser tokenParser) {
        this.tokenParser = tokenParser;
    }

    public String getUsernameFromToken(String authHeader) throws ParseException {
        String token = authHeader.substring(7);

        SignedJWT signedJWT = tokenParser.parse(token);

        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        return claimsSet.getSubject();
    }

    public String getIdFromToken(String authHeader) throws ParseException {
        String token = authHeader.substring(7);

        SignedJWT signedJWT = tokenParser.parse(token);

        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        return (String) claimsSet.getClaim("userId");
    }


    public String getRoleFromToken(String authHeader) throws ParseException {
        String token = authHeader.substring(7);

        SignedJWT signedJWT = tokenParser.parse(token);

        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

        return (String) claimsSet.getClaim("scope");
    }
}