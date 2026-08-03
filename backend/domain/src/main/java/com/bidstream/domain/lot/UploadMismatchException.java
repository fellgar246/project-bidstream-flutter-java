package com.bidstream.domain.lot;

public class UploadMismatchException extends RuntimeException {

  public UploadMismatchException() {
    super("Uploaded object does not match declared size or content type");
  }
}
