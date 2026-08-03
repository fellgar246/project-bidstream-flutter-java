package com.bidstream.application.lot.port;

import java.util.Optional;

public interface ObjectStoragePort {

  record PresignedUpload(String uploadUrl, String storageKey, int expiresInSeconds) {}

  record ObjectMetadata(long sizeBytes, String contentType) {}

  PresignedUpload presignPut(String storageKey, String contentType, int expiresInSeconds);

  Optional<ObjectMetadata> headObject(String storageKey);

  byte[] getObject(String storageKey);

  void putObject(String storageKey, byte[] data, String contentType);

  void deleteObject(String storageKey);

  String publicUrl(String storageKey);
}
