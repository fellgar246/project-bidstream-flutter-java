package com.bidstream.infrastructure.config;

import com.bidstream.application.lot.StorageProperties;
import com.bidstream.infrastructure.storage.MinioStorageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageInfrastructureConfig {

  @Bean
  MinioStorageProperties minioStorageProperties(
      @Value("${bidstream.storage.minio.endpoint:http://localhost:9000}") String endpoint,
      @Value("${bidstream.storage.minio.access-key:bidstream}") String accessKey,
      @Value("${bidstream.storage.minio.secret-key:bidstream123}") String secretKey,
      @Value("${bidstream.storage.minio.bucket:bidstream}") String bucket) {
    MinioStorageProperties properties = new MinioStorageProperties();
    properties.setEndpoint(endpoint);
    properties.setAccessKey(accessKey);
    properties.setSecretKey(secretKey);
    properties.setBucket(bucket);
    return properties;
  }

  @Bean
  StorageProperties storageProperties(
      @Value("${bidstream.storage.minio.endpoint:http://localhost:9000}") String endpoint,
      @Value("${bidstream.storage.minio.access-key:bidstream}") String accessKey,
      @Value("${bidstream.storage.minio.secret-key:bidstream123}") String secretKey,
      @Value("${bidstream.storage.minio.bucket:bidstream}") String bucket,
      @Value("${bidstream.storage.minio.presign-expiry-seconds:300}") int presignExpirySeconds) {
    StorageProperties properties = new StorageProperties();
    properties.setEndpoint(endpoint);
    properties.setAccessKey(accessKey);
    properties.setSecretKey(secretKey);
    properties.setBucket(bucket);
    properties.setPresignExpirySeconds(presignExpirySeconds);
    return properties;
  }
}
