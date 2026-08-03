package com.bidstream.application.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OutboxPort {

  OutboxEvent append(
      String aggregateType,
      long aggregateId,
      String eventType,
      Map<String, Object> payload,
      Instant occurredAt);

  List<OutboxEvent> claimUnpublished(int limit);

  void markPublished(long eventId);

  void recordFailure(long eventId, String error);

  List<OutboxEvent> findFailed();

  Optional<OutboxEvent> findById(long id);
}
