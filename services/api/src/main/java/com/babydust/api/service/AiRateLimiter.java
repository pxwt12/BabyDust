package com.babydust.api.service;

import com.babydust.api.common.RateLimitExceededException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiRateLimiter {
    private final boolean enabled;
    private final int qaLimit;
    private final int draftLimit;
    private final int preprocessLimit;
    private final long windowSeconds;
    private final Clock clock;
    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    @Autowired
    public AiRateLimiter(
            @Value("${babydust.ai.rate-limit.enabled:true}") boolean enabled,
            @Value("${babydust.ai.rate-limit.qa-per-window:30}") int qaLimit,
            @Value("${babydust.ai.rate-limit.draft-per-window:20}") int draftLimit,
            @Value("${babydust.ai.rate-limit.preprocess-per-window:10}") int preprocessLimit,
            @Value("${babydust.ai.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this(enabled, qaLimit, draftLimit, preprocessLimit, windowSeconds, Clock.systemUTC());
    }

    AiRateLimiter(boolean enabled, int qaLimit, int draftLimit, int preprocessLimit, long windowSeconds, Clock clock) {
        this.enabled = enabled;
        this.qaLimit = Math.max(1, qaLimit);
        this.draftLimit = Math.max(1, draftLimit);
        this.preprocessLimit = Math.max(1, preprocessLimit);
        this.windowSeconds = Math.max(1, windowSeconds);
        this.clock = clock;
    }

    public void check(String subjectKey, String operation) {
        if (!enabled) {
            return;
        }
        String safeSubject = subjectKey == null || subjectKey.isBlank() ? "anonymous" : subjectKey;
        String safeOperation = operation == null || operation.isBlank() ? "unknown" : operation;
        int limit = limitFor(safeOperation);
        String bucketKey = safeOperation + ":" + safeSubject;
        long now = Instant.now(clock).toEpochMilli();
        long windowMillis = windowSeconds * 1000;
        Deque<Long> bucket = buckets.computeIfAbsent(bucketKey, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && now - bucket.peekFirst() >= windowMillis) {
                bucket.removeFirst();
            }
            if (bucket.size() >= limit) {
                throw new RateLimitExceededException("AI request rate limit exceeded for " + safeOperation);
            }
            bucket.addLast(now);
        }
    }

    private int limitFor(String operation) {
        return switch (operation) {
            case "qa" -> qaLimit;
            case "ocr_report", "asr_record" -> preprocessLimit;
            default -> draftLimit;
        };
    }
}
