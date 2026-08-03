package com.bidstream.api.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public final class RabbitMqTestContainer {

  private static final GenericContainer<?> CONTAINER =
      new GenericContainer<>(DockerImageName.parse("rabbitmq:3-management"))
          .withExposedPorts(5672)
          .withEnv("RABBITMQ_DEFAULT_USER", "bidstream")
          .withEnv("RABBITMQ_DEFAULT_PASS", "bidstream");

  static {
    CONTAINER.start();
  }

  private RabbitMqTestContainer() {}

  public static GenericContainer<?> container() {
    return CONTAINER;
  }

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
      TestPropertyValues.of(
              "spring.rabbitmq.host=" + CONTAINER.getHost(),
              "spring.rabbitmq.port=" + CONTAINER.getFirstMappedPort(),
              "spring.rabbitmq.username=bidstream",
              "spring.rabbitmq.password=bidstream",
              "spring.mail.host=localhost",
              "spring.mail.port=1025")
          .applyTo(context);
    }
  }
}
