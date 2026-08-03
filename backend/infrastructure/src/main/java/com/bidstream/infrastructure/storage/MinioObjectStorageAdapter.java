package com.bidstream.infrastructure.storage;

import com.bidstream.application.lot.port.ObjectStoragePort;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class MinioObjectStorageAdapter implements ObjectStoragePort {

  private final MinioClient minioClient;
  private final String bucket;
  private final String publicEndpoint;

  public MinioObjectStorageAdapter(MinioClient minioClient, MinioStorageProperties properties) {
    this.minioClient = minioClient;
    this.bucket = properties.getBucket();
    this.publicEndpoint = normalizeEndpoint(properties.getEndpoint());
  }

  @Override
  public PresignedUpload presignPut(String storageKey, String contentType, int expiresInSeconds) {
    try {
      String uploadUrl =
          minioClient.getPresignedObjectUrl(
              io.minio.GetPresignedObjectUrlArgs.builder()
                  .method(Method.PUT)
                  .bucket(bucket)
                  .object(storageKey)
                  .expiry(expiresInSeconds, TimeUnit.SECONDS)
                  .build());
      return new PresignedUpload(uploadUrl, storageKey, expiresInSeconds);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to presign upload for " + storageKey, ex);
    }
  }

  @Override
  public Optional<ObjectMetadata> headObject(String storageKey) {
    try {
      StatObjectResponse stat =
          minioClient.statObject(
              StatObjectArgs.builder().bucket(bucket).object(storageKey).build());
      return Optional.of(new ObjectMetadata(stat.size(), stat.contentType()));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  @Override
  public byte[] getObject(String storageKey) {
    try (var stream =
        minioClient.getObject(
            GetObjectArgs.builder().bucket(bucket).object(storageKey).build())) {
      return stream.readAllBytes();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to read object " + storageKey, ex);
    }
  }

  @Override
  public void putObject(String storageKey, byte[] data, String contentType) {
    try {
      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(bucket)
              .object(storageKey)
              .stream(new ByteArrayInputStream(data), data.length, -1)
              .contentType(contentType)
              .build());
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to store object " + storageKey, ex);
    }
  }

  @Override
  public void deleteObject(String storageKey) {
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(bucket).object(storageKey).build());
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to delete object " + storageKey, ex);
    }
  }

  @Override
  public String publicUrl(String storageKey) {
    return publicEndpoint + "/" + bucket + "/" + storageKey;
  }

  private static String normalizeEndpoint(String endpoint) {
    if (endpoint.endsWith("/")) {
      return endpoint.substring(0, endpoint.length() - 1);
    }
    return endpoint;
  }
}
