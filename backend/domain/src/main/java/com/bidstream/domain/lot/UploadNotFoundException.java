package com.bidstream.domain.lot;

public class UploadNotFoundException extends RuntimeException {

  public UploadNotFoundException() {
    super("Uploaded object not found in storage");
  }
}
