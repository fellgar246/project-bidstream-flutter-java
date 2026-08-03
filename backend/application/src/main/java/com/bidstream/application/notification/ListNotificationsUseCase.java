package com.bidstream.application.notification;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListNotificationsUseCase {

  private final NotificationRepository notificationRepository;

  public ListNotificationsUseCase(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  @Transactional(readOnly = true)
  public List<Notification> execute(long userId, boolean unreadOnly, int page, int size) {
    return notificationRepository.findByUserId(userId, unreadOnly, page, size);
  }

  @Transactional(readOnly = true)
  public long countUnread(long userId) {
    return notificationRepository.countUnreadByUserId(userId);
  }
}
