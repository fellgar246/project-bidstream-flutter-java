package com.bidstream.application.realtime;

import java.time.Instant;
import java.util.Map;

/** Serializable lot event for STOMP topics and the recovery REST endpoint. */
public record LotEventEnvelope(
    long eventId, String type, long lotId, Instant occurredAt, Map<String, Object> payload) {

  public static final String BID_PLACED = "BID_PLACED";
  public static final String LOT_EXTENDED = "LOT_EXTENDED";
  public static final String LOT_STARTED = "LOT_STARTED";
  public static final String LOT_CLOSED = "LOT_CLOSED";

  /** eventId for a bid-placed event (even, monotonic per lot). */
  public static long eventIdForBid(long bidId) {
    return bidId * 2L;
  }

  /** eventId for a lot-extended event tied to the same bid (odd). */
  public static long eventIdForExtension(long bidId) {
    return bidId * 2L + 1L;
  }

  /** Synthetic event id for lot lifecycle events (high range avoids bid collisions). */
  public static long eventIdForLotLifecycle(long lotId, String type) {
    long suffix =
        switch (type) {
          case LOT_STARTED -> 1L;
          case LOT_CLOSED -> 2L;
          default -> 0L;
        };
    return lotId * 10_000L + suffix;
  }
}
