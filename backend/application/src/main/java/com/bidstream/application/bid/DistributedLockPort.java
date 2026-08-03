package com.bidstream.application.bid;

import java.time.Duration;
import java.util.Optional;

public interface DistributedLockPort {

  Duration DEFAULT_TTL = Duration.ofSeconds(2);
  Duration DEFAULT_MAX_WAIT = Duration.ofMillis(200);

  Optional<LockToken> tryAcquire(String key, Duration ttl, Duration maxWait);

  void release(LockToken token);

  record LockToken(String key, String token) {}
}
