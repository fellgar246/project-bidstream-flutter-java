package com.bidstream.api.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** Starts Postgres, Redis, and RabbitMQ for full-stack integration tests. */
public final class IntegrationTestInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(ConfigurableApplicationContext context) {
    new PostgresTestContainer.Initializer().initialize(context);
    new RedisTestContainer.Initializer().initialize(context);
    new RabbitMqTestContainer.Initializer().initialize(context);
    new MinioTestContainer.Initializer().initialize(context);
    TestPropertyValues.of("bidstream.scheduler.enabled=false").applyTo(context);
  }
}
