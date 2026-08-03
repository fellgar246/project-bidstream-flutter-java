package com.bidstream.domain.lot;

public class ImageRequiredException extends RuntimeException {

  public ImageRequiredException() {
    super("Lot must have at least one READY image before scheduling");
  }
}
