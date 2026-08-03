package com.bidstream.infrastructure.persistence.processed;

import com.bidstream.application.messaging.ProcessedEventPort;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProcessedEventRepositoryAdapter implements ProcessedEventPort {

  private final JdbcTemplate jdbcTemplate;

  public ProcessedEventRepositoryAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public boolean tryMarkProcessed(String consumer, long eventId, Instant processedAt) {
    int inserted =
        jdbcTemplate.update(
            """
            INSERT INTO processed_events (consumer, event_id, processed_at)
            VALUES (?, ?, ?)
            ON CONFLICT DO NOTHING
            """,
            consumer,
            eventId,
            java.sql.Timestamp.from(processedAt));
    return inserted == 0;
  }
}
