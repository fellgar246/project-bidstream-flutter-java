package com.bidstream.infrastructure.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

  @Bean(destroyMethod = "shutdown")
  RedisClient rateLimitRedisClient(
      @Value("${spring.data.redis.host:localhost}") String host,
      @Value("${spring.data.redis.port:6379}") int port) {
    return RedisClient.create(RedisURI.Builder.redis(host, port).build());
  }

  @Bean(destroyMethod = "close")
  StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(
      RedisClient rateLimitRedisClient) {
    RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
    return rateLimitRedisClient.connect(codec);
  }

  @Bean
  ProxyManager<String> bucket4jProxyManager(
      StatefulRedisConnection<String, byte[]> rateLimitRedisConnection) {
    return LettuceBasedProxyManager.builderFor(rateLimitRedisConnection)
        .withExpirationStrategy(
            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(1)))
        .build();
  }
}
