package com.bidstream.application.notification;

import java.time.Instant;
import java.util.Map;

public record Notification(
    long id,
    long userId,
    String type,
    Map<String, Object> payload,
    Instant readAt,
    Instant createdAt) {

  public boolean isUnread() {
    return readAt == null;
  }
}
