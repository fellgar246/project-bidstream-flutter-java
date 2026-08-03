package com.bidstream.infrastructure.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {

  @Query(
      value =
          """
          SELECT * FROM outbox_events
          WHERE published_at IS NULL AND attempts < 10
          ORDER BY id
          LIMIT :limit
          FOR UPDATE SKIP LOCKED
          """,
      nativeQuery = true)
  java.util.List<OutboxEventEntity> claimUnpublished(@Param("limit") int limit);

  java.util.List<OutboxEventEntity> findByPublishedAtIsNullAndAttemptsGreaterThanEqual(int attempts);
}
