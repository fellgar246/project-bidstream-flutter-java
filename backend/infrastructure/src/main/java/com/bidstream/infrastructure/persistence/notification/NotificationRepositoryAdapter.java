package com.bidstream.infrastructure.persistence.notification;

import com.bidstream.application.notification.Notification;
import com.bidstream.application.notification.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepositoryAdapter implements NotificationRepository {

  private final NotificationJpaRepository jpaRepository;
  private final ObjectMapper objectMapper;

  public NotificationRepositoryAdapter(
      NotificationJpaRepository jpaRepository, ObjectMapper objectMapper) {
    this.jpaRepository = jpaRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public Notification save(Notification notification) {
    NotificationEntity entity =
        notification.id() == 0L
            ? new NotificationEntity()
            : jpaRepository.findById(notification.id()).orElseThrow();
    if (notification.id() == 0L) {
      entity.setCreatedAt(notification.createdAt());
    } else {
      entity.setCreatedAt(notification.createdAt());
    }
    entity.setUserId(notification.userId());
    entity.setType(notification.type());
    entity.setPayload(writeJson(notification.payload()));
    entity.setReadAt(notification.readAt());
    return toDomain(jpaRepository.save(entity));
  }

  @Override
  public List<Notification> findByUserId(long userId, boolean unreadOnly, int page, int size) {
    return jpaRepository
        .findByUser(userId, unreadOnly, PageRequest.of(page, size))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public long countUnreadByUserId(long userId) {
    return jpaRepository.countByUserIdAndReadAtIsNull(userId);
  }

  @Override
  public Optional<Notification> findByIdAndUserId(long id, long userId) {
    return jpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
  }

  @Override
  public int markAllRead(long userId) {
    return jpaRepository.markAllRead(userId, java.time.Instant.now());
  }

  private Notification toDomain(NotificationEntity entity) {
    return new Notification(
        entity.getId(),
        entity.getUserId(),
        entity.getType(),
        readJson(entity.getPayload()),
        entity.getReadAt(),
        entity.getCreatedAt());
  }

  private String writeJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Invalid notification payload", ex);
    }
  }

  private Map<String, Object> readJson(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException ex) {
      return new LinkedHashMap<>();
    }
  }
}
