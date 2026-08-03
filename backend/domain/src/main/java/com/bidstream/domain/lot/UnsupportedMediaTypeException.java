package com.bidstream.domain.lot;

public class UnsupportedMediaTypeException extends RuntimeException {

  public UnsupportedMediaTypeException(String contentType) {
    super("Unsupported content type: " + contentType);
  }
}
