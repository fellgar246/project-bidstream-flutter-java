package com.bidstream.domain.auth;

public class AlreadySellerException extends RuntimeException {

  public AlreadySellerException() {
    super("User is already a seller");
  }
}
