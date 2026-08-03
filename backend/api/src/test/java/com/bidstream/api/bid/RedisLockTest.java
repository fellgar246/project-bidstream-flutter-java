package com.bidstream.api.bid;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidstream.application.bid.DistributedLockPort;
import com.bidstream.infrastructure.lock.RedisDistributedLockAdapter;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import com.bidstream.api.support.PostgresTestContainer;
import com.bidstream.api.support.RedisTestContainer;

@SpringBootTest
@ContextConfiguration(
    initializers = {
      PostgresTestContainer.Initializer.class,
      RedisTestContainer.Initializer.class
    })
class RedisLockTest {

  @Autowired private RedisDistributedLockAdapter lockAdapter;
  @Autowired private StringRedisTemplate redisTemplate;

  @Test
  void ca0515_expiredTokenDoesNotReleaseNewOwnerLock() {
    String key = "lock:test:token-ownership";
    redisTemplate.delete(key);

    Optional<DistributedLockPort.LockToken> first =
        lockAdapter.tryAcquire(key, Duration.ofMillis(50), Duration.ofMillis(100));
    assertThat(first).isPresent();

    sleep(60);

    Optional<DistributedLockPort.LockToken> second =
        lockAdapter.tryAcquire(key, Duration.ofSeconds(2), Duration.ofMillis(200));
    assertThat(second).isPresent();
    assertThat(second.get().token()).isNotEqualTo(first.get().token());

    lockAdapter.release(first.get());

    String owner = redisTemplate.opsForValue().get(key);
    assertThat(owner).isEqualTo(second.get().token());

    lockAdapter.release(second.get());
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
