package com.bidstream.application.messaging;

import java.time.Instant;

public interface ProcessedEventPort {

  /** Returns true if this consumer already processed the event (duplicate). */
  boolean tryMarkProcessed(String consumer, long eventId, Instant processedAt);
}
