package com.bidstream.application.bid;

import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Degraded mode: proceed without a real lock when Redis is unavailable. */
@Component
public class PassthroughDistributedLockAdapter implements DistributedLockPort {

  @Override
  public Optional<LockToken> tryAcquire(String key, Duration ttl, Duration maxWait) {
    return Optional.of(new LockToken(key, "passthrough"));
  }

  @Override
  public void release(LockToken token) {
    // no-op
  }
}
