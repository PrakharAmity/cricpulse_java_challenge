package com.cricpulse.service;

import org.springframework.stereotype.Service;

@Service
public class PollLimiterService {

    // Intentional Bug 6: Ignores userId and maintains a single static class-level window counter.
    // When User A exhausts their 3-vote limit, User B is immediately locked out.
    private static long windowStarted;
    private static int requestsInWindow;

    public synchronized boolean allowFanPoll(String userId, long nowMs) {
        if (nowMs - windowStarted >= 10_000) {
            windowStarted = nowMs;
            requestsInWindow = 0;
        }
        requestsInWindow++;
        return requestsInWindow <= 3;
    }

    public synchronized void reset() {
        windowStarted = 0;
        requestsInWindow = 0;
    }
}
