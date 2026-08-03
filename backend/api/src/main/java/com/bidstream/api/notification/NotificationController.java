package com.bidstream.api.notification;

import com.bidstream.application.notification.ListNotificationsUseCase;
import com.bidstream.application.notification.MarkAllNotificationsReadUseCase;
import com.bidstream.application.notification.MarkNotificationReadUseCase;
import com.bidstream.application.notification.Notification;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/notifications")
public class NotificationController {

  private final ListNotificationsUseCase listNotificationsUseCase;
  private final MarkNotificationReadUseCase markNotificationReadUseCase;
  private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;

  public NotificationController(
      ListNotificationsUseCase listNotificationsUseCase,
      MarkNotificationReadUseCase markNotificationReadUseCase,
      MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase) {
    this.listNotificationsUseCase = listNotificationsUseCase;
    this.markNotificationReadUseCase = markNotificationReadUseCase;
    this.markAllNotificationsReadUseCase = markAllNotificationsReadUseCase;
  }

  @GetMapping
  public NotificationListResponse list(
      Authentication authentication,
      @RequestParam(defaultValue = "false") boolean unreadOnly,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    long userId = (long) authentication.getPrincipal();
    List<Notification> items = listNotificationsUseCase.execute(userId, unreadOnly, page, size);
    long unreadCount = listNotificationsUseCase.countUnread(userId);
    return new NotificationListResponse(
        items.stream().map(NotificationResponse::from).toList(), unreadCount);
  }

  @PostMapping("/{id}/read")
  public NotificationResponse markRead(Authentication authentication, @PathVariable long id) {
    long userId = (long) authentication.getPrincipal();
    return NotificationResponse.from(markNotificationReadUseCase.execute(userId, id));
  }

  @PostMapping("/read-all")
  public Map<String, Integer> markAllRead(Authentication authentication) {
    long userId = (long) authentication.getPrincipal();
    int updated = markAllNotificationsReadUseCase.execute(userId);
    return Map.of("markedRead", updated);
  }

  public record NotificationListResponse(List<NotificationResponse> items, long unreadCount) {}

  public record NotificationResponse(
      long id, String type, Map<String, Object> payload, String readAt, String createdAt) {

    static NotificationResponse from(Notification notification) {
      return new NotificationResponse(
          notification.id(),
          notification.type(),
          notification.payload(),
          notification.readAt() != null ? notification.readAt().toString() : null,
          notification.createdAt().toString());
    }
  }
}
