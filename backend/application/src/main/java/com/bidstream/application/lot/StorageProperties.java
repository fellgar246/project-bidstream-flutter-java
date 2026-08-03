package com.bidstream.application.lot;

public class StorageProperties {

  private String endpoint = "http://localhost:9000";
  private String accessKey = "bidstream";
  private String secretKey = "bidstream123";
  private String bucket = "bidstream";
  private int presignExpirySeconds = 300;

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public int getPresignExpirySeconds() {
    return presignExpirySeconds;
  }

  public void setPresignExpirySeconds(int presignExpirySeconds) {
    this.presignExpirySeconds = presignExpirySeconds;
  }
}
