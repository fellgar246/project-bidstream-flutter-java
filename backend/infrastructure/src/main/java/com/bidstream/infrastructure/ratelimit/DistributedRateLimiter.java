package com.bidstream.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class DistributedRateLimiter {

  private static final Bandwidth BID_LIMIT =
      Bandwidth.classic(10, Refill.intervally(10, Duration.ofSeconds(10)));
  private static final Bandwidth PUBLIC_READ_LIMIT =
      Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1)));
  private static final Bandwidth LOGIN_LIMIT =
      Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(5)));

  private final ProxyManager<String> proxyManager;

  public DistributedRateLimiter(ProxyManager<String> proxyManager) {
    this.proxyManager = proxyManager;
  }

  public RateLimitDecision tryConsumeBid(long userId, long lotId) {
    return consume("rl:bid:" + userId + ":" + lotId, BID_LIMIT);
  }

  public RateLimitDecision tryConsumePublicRead(String clientIp) {
    return consume("rl:public:" + clientIp, PUBLIC_READ_LIMIT);
  }

  public RateLimitDecision tryConsumeLogin(String clientIp, String email) {
    String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
    return consume("rl:login:" + clientIp + ":" + normalizedEmail, LOGIN_LIMIT);
  }

  private RateLimitDecision consume(String key, Bandwidth limit) {
    Bucket bucket =
        proxyManager
            .builder()
            .build(key, () -> BucketConfiguration.builder().addLimit(limit).build());
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    Duration retryAfter =
        probe.isConsumed()
            ? Duration.ZERO
            : Duration.ofNanos(Math.max(probe.getNanosToWaitForRefill(), 0));
    return new RateLimitDecision(probe.isConsumed(), probe.getRemainingTokens(), retryAfter);
  }

  public record RateLimitDecision(boolean allowed, long remaining, Duration retryAfter) {}
}
