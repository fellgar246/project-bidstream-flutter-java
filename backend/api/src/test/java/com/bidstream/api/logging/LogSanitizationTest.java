package com.bidstream.api.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogSanitizationTest {

  @Test
  void ca105_rejectsBearerTokens() {
    assertThat(
            LogSanitizationTurboFilter.containsSensitive(
                "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.abc.def"))
        .isTrue();
  }

  @Test
  void ca105_rejectsPasswordFields() {
    assertThat(LogSanitizationTurboFilter.containsSensitive("password=secret123")).isTrue();
  }

  @Test
  void ca105_rejectsSignedUrls() {
    assertThat(
            LogSanitizationTurboFilter.containsSensitive(
                "https://minio.local/bidstream/key?X-Amz-Signature=abc123"))
        .isTrue();
  }

  @Test
  void ca105_rejectsFullEmails() {
    assertThat(LogSanitizationTurboFilter.containsSensitive("user@example.com logged in")).isTrue();
  }

  @Test
  void ca105_allowsSafeMessages() {
    assertThat(
            LogSanitizationTurboFilter.containsSensitive("Placing bid on lotId=42 amount=100.00"))
        .isFalse();
  }
}
