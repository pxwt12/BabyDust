package com.babydust.api.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.babydust.api.common.RateLimitExceededException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AiRateLimiterTest {
    @Test
    void limitsQaRequestsWithinWindow() {
        AiRateLimiter limiter = new AiRateLimiter(true, 2, 10, 10, 60, Clock.fixed(Instant.parse("2026-05-26T00:00:00Z"), ZoneOffset.UTC));

        limiter.check("user:test", "qa");
        limiter.check("user:test", "qa");

        assertThatThrownBy(() -> limiter.check("user:test", "qa"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("qa");
    }

    @Test
    void separatesOperationsAndSubjects() {
        AiRateLimiter limiter = new AiRateLimiter(true, 1, 1, 1, 60, Clock.fixed(Instant.parse("2026-05-26T00:00:00Z"), ZoneOffset.UTC));

        limiter.check("user:a", "qa");
        limiter.check("user:a", "extract_record");
        limiter.check("user:b", "qa");

        assertThatThrownBy(() -> limiter.check("user:a", "qa"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void disabledLimiterAlwaysAllowsRequests() {
        AiRateLimiter limiter = new AiRateLimiter(false, 1, 1, 1, 60, Clock.fixed(Instant.parse("2026-05-26T00:00:00Z"), ZoneOffset.UTC));

        assertThatCode(() -> {
            limiter.check("user:test", "qa");
            limiter.check("user:test", "qa");
            limiter.check("user:test", "qa");
        }).doesNotThrowAnyException();
    }
}
