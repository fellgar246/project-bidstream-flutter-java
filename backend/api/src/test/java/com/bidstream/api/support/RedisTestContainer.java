package com.bidstream.api.support;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public final class RedisTestContainer {

  private static final RedisContainer CONTAINER = new RedisContainer("redis:7-alpine");

  static {
    CONTAINER.start();
  }

  private RedisTestContainer() {}

  public static RedisContainer container() {
    return CONTAINER;
  }

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
      TestPropertyValues.of(
              "spring.data.redis.host=" + CONTAINER.getHost(),
              "spring.data.redis.port=" + CONTAINER.getFirstMappedPort())
          .applyTo(context);
    }
  }
}
