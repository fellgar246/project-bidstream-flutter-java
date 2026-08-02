package com.bidstream.api.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public final class PostgresTestContainer {

  private static final PostgreSQLContainer<?> CONTAINER =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("bidstream_test")
          .withUsername("bidstream")
          .withPassword("bidstream");

  static {
    CONTAINER.start();
  }

  private PostgresTestContainer() {}

  public static PostgreSQLContainer<?> container() {
    return CONTAINER;
  }

  @TestConfiguration
  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
      TestPropertyValues.of(
              "spring.datasource.url=" + CONTAINER.getJdbcUrl(),
              "spring.datasource.username=" + CONTAINER.getUsername(),
              "spring.datasource.password=" + CONTAINER.getPassword(),
              "spring.flyway.enabled=true",
              "spring.jpa.hibernate.ddl-auto=validate")
          .applyTo(context);
    }
  }
}
