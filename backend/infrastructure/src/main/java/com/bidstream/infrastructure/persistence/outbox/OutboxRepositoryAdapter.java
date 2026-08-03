package com.bidstream.infrastructure.persistence.outbox;

import com.bidstream.application.outbox.OutboxEvent;
import com.bidstream.application.outbox.OutboxPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OutboxRepositoryAdapter implements OutboxPort {

  private static final int MAX_ATTEMPTS = 10;

  private final OutboxEventJpaRepository jpaRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public OutboxRepositoryAdapter(
      OutboxEventJpaRepository jpaRepository, ObjectMapper objectMapper, Clock clock) {
    this.jpaRepository = jpaRepository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Override
  @Transactional
  public OutboxEvent append(
      String aggregateType,
      long aggregateId,
      String eventType,
      Map<String, Object> payload,
      Instant occurredAt) {
    OutboxEventEntity entity = new OutboxEventEntity();
    entity.setAggregateType(aggregateType);
    entity.setAggregateId(aggregateId);
    entity.setEventType(eventType);
    entity.setPayload(writeJson(payload));
    entity.setOccurredAt(occurredAt);
    entity.setAttempts(0);
    OutboxEventEntity saved = jpaRepository.save(entity);
    return toDomain(saved);
  }

  @Override
  @Transactional
  public List<OutboxEvent> claimUnpublished(int limit) {
    return jpaRepository.claimUnpublished(limit).stream().map(this::toDomain).toList();
  }

  @Override
  @Transactional
  public void markPublished(long eventId) {
    OutboxEventEntity entity = jpaRepository.findById(eventId).orElseThrow();
    entity.setPublishedAt(clock.instant());
    jpaRepository.save(entity);
  }

  @Override
  @Transactional
  public void recordFailure(long eventId, String error) {
    OutboxEventEntity entity = jpaRepository.findById(eventId).orElseThrow();
    entity.setAttempts(entity.getAttempts() + 1);
    entity.setLastError(error);
    jpaRepository.save(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OutboxEvent> findFailed() {
    return jpaRepository.findByPublishedAtIsNullAndAttemptsGreaterThanEqual(MAX_ATTEMPTS).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long countPending() {
    return jpaRepository.countByPublishedAtIsNull();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasStuckEvents() {
    return jpaRepository.existsByPublishedAtIsNullAndAttemptsGreaterThanEqual(MAX_ATTEMPTS);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<OutboxEvent> findById(long id) {
    return jpaRepository.findById(id).map(this::toDomain);
  }

  private OutboxEvent toDomain(OutboxEventEntity entity) {
    return new OutboxEvent(
        entity.getId(),
        entity.getAggregateType(),
        entity.getAggregateId(),
        entity.getEventType(),
        readJson(entity.getPayload()),
        entity.getOccurredAt(),
        entity.getPublishedAt(),
        entity.getAttempts(),
        entity.getLastError());
  }

  private String writeJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Invalid outbox payload", ex);
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
