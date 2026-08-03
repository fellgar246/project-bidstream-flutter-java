package com.bidstream.infrastructure.persistence.processed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "processed_events")
@IdClass(ProcessedEventEntity.ProcessedEventId.class)
public class ProcessedEventEntity {

  @Id
  @Column(nullable = false)
  private String consumer;

  @Id
  @Column(name = "event_id", nullable = false)
  private long eventId;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  public String getConsumer() {
    return consumer;
  }

  public void setConsumer(String consumer) {
    this.consumer = consumer;
  }

  public long getEventId() {
    return eventId;
  }

  public void setEventId(long eventId) {
    this.eventId = eventId;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(Instant processedAt) {
    this.processedAt = processedAt;
  }

  public static class ProcessedEventId implements Serializable {
    private String consumer;
    private long eventId;

    public ProcessedEventId() {}

    public ProcessedEventId(String consumer, long eventId) {
      this.consumer = consumer;
      this.eventId = eventId;
    }

    public String getConsumer() {
      return consumer;
    }

    public void setConsumer(String consumer) {
      this.consumer = consumer;
    }

    public long getEventId() {
      return eventId;
    }

    public void setEventId(long eventId) {
      this.eventId = eventId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ProcessedEventId that = (ProcessedEventId) o;
      return eventId == that.eventId && Objects.equals(consumer, that.consumer);
    }

    @Override
    public int hashCode() {
      return Objects.hash(consumer, eventId);
    }
  }
}
