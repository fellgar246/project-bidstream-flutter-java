package com.bidstream.application.auth.port;

public interface PasswordEncoderPort {

  String hash(String rawPassword);

  boolean matches(String rawPassword, String encodedPassword);
}
