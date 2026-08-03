package com.bidstream.application.notification;

import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkNotificationReadUseCase {

  private final NotificationRepository notificationRepository;
  private final Clock clock;

  public MarkNotificationReadUseCase(NotificationRepository notificationRepository, Clock clock) {
    this.notificationRepository = notificationRepository;
    this.clock = clock;
  }

  @Transactional
  public Notification execute(long userId, long notificationId) {
    Notification existing =
        notificationRepository
            .findByIdAndUserId(notificationId, userId)
            .orElseThrow(() -> new NoSuchElementException("Notification not found"));
    if (!existing.isUnread()) {
      return existing;
    }
    Instant now = clock.instant();
    Notification updated =
        new Notification(
            existing.id(),
            existing.userId(),
            existing.type(),
            existing.payload(),
            now,
            existing.createdAt());
    return notificationRepository.save(updated);
  }
}
