package com.bidstream.infrastructure.cache;

import com.bidstream.application.cache.CachedLotPageSnapshot;
import com.bidstream.application.cache.CachedLotSnapshot;
import com.bidstream.application.cache.CategoryCachePort;
import com.bidstream.application.cache.LotCachePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisLotCacheAdapter implements LotCachePort {

  public static final String LOT_KEY_PREFIX = "lot:";
  static final String LOT_PAGE_KEY_PREFIX = "lots:page:";
  static final String CATALOG_VERSION_KEY = "catalog:version";

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  public RedisLotCacheAdapter(StringRedisTemplate redis, ObjectMapper objectMapper) {
    this.redis = redis;
    this.objectMapper = objectMapper;
  }

  @Override
  public Optional<CachedLotSnapshot> getLot(long lotId) {
    return readJson(LOT_KEY_PREFIX + lotId, CachedLotSnapshot.class);
  }

  @Override
  public void putLot(long lotId, CachedLotSnapshot snapshot, Duration ttl) {
    writeJson(LOT_KEY_PREFIX + lotId, snapshot, ttl);
  }

  @Override
  public void invalidateLot(long lotId) {
    redis.delete(LOT_KEY_PREFIX + lotId);
  }

  @Override
  public Optional<CachedLotPageSnapshot> getLotPage(String pageKey) {
    return readJson(LOT_PAGE_KEY_PREFIX + pageKey, CachedLotPageSnapshot.class);
  }

  @Override
  public void putLotPage(String pageKey, CachedLotPageSnapshot page, Duration ttl) {
    writeJson(LOT_PAGE_KEY_PREFIX + pageKey, page, ttl);
  }

  @Override
  public long getCatalogVersion() {
    String value = redis.opsForValue().get(CATALOG_VERSION_KEY);
    if (value == null || value.isBlank()) {
      return 0L;
    }
    return Long.parseLong(value);
  }

  @Override
  public long incrementCatalogVersion() {
    Long next = redis.opsForValue().increment(CATALOG_VERSION_KEY);
    return next != null ? next : 0L;
  }

  private <T> Optional<T> readJson(String key, Class<T> type) {
    String json = redis.opsForValue().get(key);
    if (json == null || json.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(json, type));
    } catch (JsonProcessingException ex) {
      redis.delete(key);
      return Optional.empty();
    }
  }

  private void writeJson(String key, Object value, Duration ttl) {
    try {
      String json = objectMapper.writeValueAsString(value);
      redis.opsForValue().set(key, json, ttl);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize cache entry for " + key, ex);
    }
  }
}

@Component
class RedisCategoryCacheAdapter implements CategoryCachePort {

  static final String CATEGORIES_TREE_KEY = "categories:tree";

  private final StringRedisTemplate redis;

  RedisCategoryCacheAdapter(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Optional<String> getTreeJson() {
    return Optional.ofNullable(redis.opsForValue().get(CATEGORIES_TREE_KEY));
  }

  @Override
  public void putTreeJson(String json, Duration ttl) {
    redis.opsForValue().set(CATEGORIES_TREE_KEY, json, ttl);
  }

  @Override
  public void invalidate() {
    redis.delete(CATEGORIES_TREE_KEY);
  }
}
