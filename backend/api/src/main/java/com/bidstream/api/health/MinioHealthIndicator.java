package com.bidstream.api.health;

import com.bidstream.application.lot.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("minio")
public class MinioHealthIndicator implements HealthIndicator {

  private final MinioClient minioClient;
  private final String bucket;

  public MinioHealthIndicator(MinioClient minioClient, StorageProperties storageProperties) {
    this.minioClient = minioClient;
    this.bucket = storageProperties.getBucket();
  }

  @Override
  public Health health() {
    try {
      boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      if (exists) {
        return Health.up().withDetail("bucket", bucket).build();
      }
      return Health.down().withDetail("bucket", bucket).withDetail("reason", "missing").build();
    } catch (Exception ex) {
      return Health.down(ex).build();
    }
  }
}
