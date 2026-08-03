package com.bidstream.domain.auth;

public class TokenReuseException extends RuntimeException {

  public TokenReuseException() {
    super("Refresh token reuse detected");
  }
}
