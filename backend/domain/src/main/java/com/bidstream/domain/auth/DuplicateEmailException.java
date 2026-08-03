package com.bidstream.domain.auth;

public class DuplicateEmailException extends RuntimeException {

  public DuplicateEmailException(String email) {
    super("Email already registered: " + email);
  }
}
