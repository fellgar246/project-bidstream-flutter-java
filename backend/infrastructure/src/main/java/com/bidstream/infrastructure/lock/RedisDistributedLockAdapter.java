package com.bidstream.infrastructure.lock;

import com.bidstream.application.bid.DistributedLockPort;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@Primary
public class RedisDistributedLockAdapter implements DistributedLockPort {

  private static final Logger log = LoggerFactory.getLogger(RedisDistributedLockAdapter.class);

  private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
      new DefaultRedisScript<>(
          """
          if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
          else
            return 0
          end
          """,
          Long.class);

  private final StringRedisTemplate redisTemplate;

  public RedisDistributedLockAdapter(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public Optional<LockToken> tryAcquire(String key, Duration ttl, Duration maxWait) {
    try {
      String token = UUID.randomUUID().toString();
      long deadline = System.nanoTime() + maxWait.toNanos();
      while (System.nanoTime() < deadline) {
        Boolean acquired =
            redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        if (Boolean.TRUE.equals(acquired)) {
          return Optional.of(new LockToken(key, token));
        }
        Thread.sleep(5);
      }
      return Optional.empty();
    } catch (RedisConnectionFailureException | QueryTimeoutException ex) {
      log.warn("Redis unavailable, proceeding without distributed lock: {}", ex.getMessage());
      return Optional.of(new LockToken(key, "degraded"));
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }

  @Override
  public void release(LockToken token) {
    if ("degraded".equals(token.token()) || "passthrough".equals(token.token())) {
      return;
    }
    try {
      redisTemplate.execute(RELEASE_SCRIPT, java.util.List.of(token.key()), token.token());
    } catch (RedisConnectionFailureException ex) {
      log.warn("Redis unavailable during lock release: {}", ex.getMessage());
    }
  }
}
