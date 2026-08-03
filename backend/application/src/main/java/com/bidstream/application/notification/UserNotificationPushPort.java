package com.bidstream.application.notification;

public interface UserNotificationPushPort {

  void push(long userId, Notification notification);
}
