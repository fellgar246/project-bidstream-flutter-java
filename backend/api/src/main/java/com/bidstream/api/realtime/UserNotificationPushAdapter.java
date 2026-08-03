package com.bidstream.api.realtime;

import com.bidstream.application.notification.Notification;
import com.bidstream.application.notification.UserNotificationPushPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Primary
public class UserNotificationPushAdapter implements UserNotificationPushPort {

  private final SimpMessagingTemplate messagingTemplate;

  public UserNotificationPushAdapter(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void push(long userId, Notification notification) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", notification.id());
    body.put("type", notification.type());
    body.put("payload", notification.payload());
    body.put("createdAt", notification.createdAt().toString());
    messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", body);
  }
}
