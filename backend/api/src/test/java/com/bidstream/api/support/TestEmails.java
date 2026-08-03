package com.bidstream.api.support;

import java.util.UUID;

public final class TestEmails {

  private TestEmails() {}

  public static String unique(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@test.com";
  }
}
