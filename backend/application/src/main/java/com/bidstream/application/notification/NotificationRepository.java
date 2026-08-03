package com.bidstream.application.notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

  Notification save(Notification notification);

  List<Notification> findByUserId(long userId, boolean unreadOnly, int page, int size);

  long countUnreadByUserId(long userId);

  Optional<Notification> findByIdAndUserId(long id, long userId);

  int markAllRead(long userId);
}
