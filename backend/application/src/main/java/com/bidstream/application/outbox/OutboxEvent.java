package com.bidstream.application.outbox;

import java.time.Instant;
import java.util.Map;

/** Integration events written to the transactional outbox (RB-09). */
public record OutboxEvent(
    long id,
    String aggregateType,
    long aggregateId,
    String eventType,
    Map<String, Object> payload,
    Instant occurredAt,
    Instant publishedAt,
    int attempts,
    String lastError) {

  public static final String AGGREGATE_LOT = "Lot";

  public static final String BID_PLACED = "BidPlaced";
  public static final String LOT_STARTED = "LotStarted";
  public static final String LOT_CLOSED_SOLD = "LotClosedSold";
  public static final String LOT_CLOSED_NO_SALE = "LotClosedNoSale";
  public static final String LOT_EXTENDED = "LotExtended";

  public static String routingKey(String eventType) {
    return switch (eventType) {
      case BID_PLACED -> "bid.placed";
      case LOT_STARTED -> "lot.started";
      case LOT_CLOSED_SOLD -> "lot.closed.sold";
      case LOT_CLOSED_NO_SALE -> "lot.closed.no_sale";
      case LOT_EXTENDED -> "lot.extended";
      default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
    };
  }

  public boolean isFailed() {
    return publishedAt == null && attempts >= 10;
  }
}
