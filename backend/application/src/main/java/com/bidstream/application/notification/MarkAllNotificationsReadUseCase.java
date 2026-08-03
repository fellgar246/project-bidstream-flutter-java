package com.bidstream.application.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkAllNotificationsReadUseCase {

  private final NotificationRepository notificationRepository;

  public MarkAllNotificationsReadUseCase(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  @Transactional
  public int execute(long userId) {
    return notificationRepository.markAllRead(userId);
  }
}
