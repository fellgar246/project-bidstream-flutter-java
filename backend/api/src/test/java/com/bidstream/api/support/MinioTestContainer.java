package com.bidstream.api.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class MinioTestContainer {

  public static final String ACCESS_KEY = "bidstream";
  public static final String SECRET_KEY = "bidstream123";
  public static final String BUCKET = "bidstream-test";

  private static final GenericContainer<?> CONTAINER =
      new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
          .withCommand("server", "/data", "--console-address", ":9001")
          .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
          .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

  static {
    CONTAINER.start();
  }

  private MinioTestContainer() {}

  public static GenericContainer<?> container() {
    return CONTAINER;
  }

  public static String endpoint() {
    return "http://" + CONTAINER.getHost() + ":" + CONTAINER.getMappedPort(9000);
  }

  static void createBucketIfNeeded() {
    try {
      io.minio.MinioClient client =
          io.minio.MinioClient.builder()
              .endpoint(endpoint())
              .credentials(ACCESS_KEY, SECRET_KEY)
              .build();
      boolean exists =
          client.bucketExists(io.minio.BucketExistsArgs.builder().bucket(BUCKET).build());
      if (!exists) {
        client.makeBucket(io.minio.MakeBucketArgs.builder().bucket(BUCKET).build());
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to initialize MinIO test bucket", ex);
    }
  }

  @TestConfiguration
  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
      createBucketIfNeeded();
      TestPropertyValues.of(
              "bidstream.storage.minio.endpoint=" + endpoint(),
              "bidstream.storage.minio.access-key=" + ACCESS_KEY,
              "bidstream.storage.minio.secret-key=" + SECRET_KEY,
              "bidstream.storage.minio.bucket=" + BUCKET)
          .applyTo(context);
    }
  }
}
