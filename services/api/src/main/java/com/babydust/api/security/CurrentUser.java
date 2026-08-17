package com.babydust.api.security;

import com.babydust.api.common.UnauthorizedException;
import com.babydust.api.repository.SessionTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final SessionTokenRepository tokens;
    private final boolean devUserHeaderEnabled;

    public CurrentUser(SessionTokenRepository tokens, @Value("${babydust.security.dev-user-header-enabled:false}") boolean devUserHeaderEnabled) {
        this.tokens = tokens;
        this.devUserHeaderEnabled = devUserHeaderEnabled;
    }

    public UUID id(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (devUserHeaderEnabled && userId != null && !userId.isBlank()) {
            return UUID.fromString(userId);
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length());
            return tokens.findByToken(token)
                    .filter(session -> session.getExpiresAt().isAfter(OffsetDateTime.now()))
                    .map(session -> session.getUserId())
                    .orElseThrow(() -> new UnauthorizedException("Invalid or expired token"));
        }
        throw new UnauthorizedException("Missing user session");
    }
}
