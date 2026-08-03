package com.bidstream.infrastructure.realtime;

import com.bidstream.application.realtime.LotPresencePort;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisLotPresenceStore implements LotPresencePort {

  private static final String KEY_PREFIX = "presence:lot:";

  private final StringRedisTemplate redis;
  private final Duration ttl;

  public RedisLotPresenceStore(
      StringRedisTemplate redis,
      @Value("${bidstream.realtime.presence-ttl-seconds:30}") long ttlSeconds) {
    this.redis = redis;
    this.ttl = Duration.ofSeconds(ttlSeconds);
  }

  @Override
  public void markPresent(long lotId, long userId) {
    refresh(lotId, userId);
  }

  @Override
  public void markAbsent(long lotId, long userId) {
    redis.opsForZSet().remove(key(lotId), member(userId));
  }

  @Override
  public void refresh(long lotId, long userId) {
    double expiresAt = Instant.now().plus(ttl).toEpochMilli();
    redis.opsForZSet().add(key(lotId), member(userId), expiresAt);
  }

  @Override
  public int countWatching(long lotId) {
    String redisKey = key(lotId);
    double now = Instant.now().toEpochMilli();
    redis.opsForZSet().removeRangeByScore(redisKey, Double.NEGATIVE_INFINITY, now);
    Long count = redis.opsForZSet().zCard(redisKey);
    return count == null ? 0 : count.intValue();
  }

  private static String key(long lotId) {
    return KEY_PREFIX + lotId;
  }

  private static String member(long userId) {
    return String.valueOf(userId);
  }
}
