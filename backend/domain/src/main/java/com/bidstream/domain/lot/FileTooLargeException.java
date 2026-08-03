package com.bidstream.domain.lot;

public class FileTooLargeException extends RuntimeException {

  public FileTooLargeException(long sizeBytes, long maxBytes) {
    super("File size " + sizeBytes + " exceeds maximum " + maxBytes);
  }
}
