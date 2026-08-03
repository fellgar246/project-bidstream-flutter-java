package com.bidstream.domain.lot;

public class ImageLimitReachedException extends RuntimeException {

  public ImageLimitReachedException(int limit) {
    super("Lot already has the maximum of " + limit + " images");
  }
}
