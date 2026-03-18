package com.placementgo.backend.resume.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token-aware rate limiter for Groq's free tier.
 * Groq free tier limit: 12,000 tokens per minute (TPM) for llama-3.3-70b-versatile.
 * We use 10,500 as the effective limit to leave headroom.
 */
@Component
@Slf4j
public class GroqRateLimiter {

    private static final int TPM_LIMIT = 12_000;
    private static final long WINDOW_MS = 60_000L;

    private volatile long windowStart = System.currentTimeMillis();
    private final AtomicInteger usedTokens = new AtomicInteger(0);
    private final Object lock = new Object();

    /**
     * Blocks until there is token budget available for the estimated request size.
     * Resets the window automatically when 60 seconds have elapsed.
     */
    public void acquireTokens(int estimatedTokens) throws InterruptedException {
        synchronized (lock) {
            while (true) {
                long now = System.currentTimeMillis();
                long elapsed = now - windowStart;

                if (elapsed >= WINDOW_MS) {
                    windowStart = now;
                    usedTokens.set(0);
                    log.info("🔄 Token window reset. {} tokens available.", TPM_LIMIT);
                }

                int current = usedTokens.get();
                if (current + estimatedTokens <= TPM_LIMIT) {
                    usedTokens.addAndGet(estimatedTokens);
                    log.info("🎟️ Token budget: ~{} tokens reserved. Running total: {}/{} TPM",
                            estimatedTokens, usedTokens.get(), TPM_LIMIT);
                    return;
                }

                long waitMs = WINDOW_MS - elapsed + 200;
                log.warn("⏳ TPM limit approaching ({}/{}). Queuing request — waiting {}ms.",
                        current, TPM_LIMIT, waitMs);
                lock.wait(waitMs);
            }
        }
    }

    /**
     * Rough token estimate: 4 chars ≈ 1 token for input, plus 1500 for expected output.
     */
    public int estimateTokens(String requestBody) {
        return (requestBody.length() / 4) + 4000;
    }
}
