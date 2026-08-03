package com.bidstream.application.outbox;

import com.bidstream.application.tracing.TraceContext;
import com.bidstream.domain.bid.Bid;
import com.bidstream.domain.lot.Lot;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class OutboxWriter {

  private final OutboxPort outboxPort;

  public OutboxWriter(OutboxPort outboxPort) {
    this.outboxPort = outboxPort;
  }

  public void writeBidPlaced(
      Lot lot, Bid bid, Optional<Long> previousLeaderId, Instant occurredAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("lotId", lot.id());
    payload.put("bidId", bid.id());
    payload.put("bidderId", bid.bidderId());
    payload.put("amountCents", bid.amount().cents());
    previousLeaderId.ifPresent(id -> payload.put("previousLeaderId", id));
    enrichWithTraceContext(payload);
    outboxPort.append(
        OutboxEvent.AGGREGATE_LOT, lot.id(), OutboxEvent.BID_PLACED, payload, occurredAt);
  }

  public void writeLotExtended(Lot lot, Instant newEndAt, int extensionCount, Instant occurredAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("lotId", lot.id());
    payload.put("newEndAt", newEndAt.toString());
    payload.put("extensionCount", extensionCount);
    enrichWithTraceContext(payload);
    outboxPort.append(
        OutboxEvent.AGGREGATE_LOT, lot.id(), OutboxEvent.LOT_EXTENDED, payload, occurredAt);
  }

  public void writeLotStarted(Lot lot, Instant startedAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("lotId", lot.id());
    payload.put("sellerId", lot.sellerId());
    payload.put("startedAt", startedAt.toString());
    enrichWithTraceContext(payload);
    outboxPort.append(
        OutboxEvent.AGGREGATE_LOT, lot.id(), OutboxEvent.LOT_STARTED, payload, startedAt);
  }

  public void writeLotClosedSold(
      Lot lot, long winningBidId, long winnerId, long amountCents, Instant closedAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("lotId", lot.id());
    payload.put("winningBidId", winningBidId);
    payload.put("winnerId", winnerId);
    payload.put("sellerId", lot.sellerId());
    payload.put("amountCents", amountCents);
    enrichWithTraceContext(payload);
    outboxPort.append(
        OutboxEvent.AGGREGATE_LOT, lot.id(), OutboxEvent.LOT_CLOSED_SOLD, payload, closedAt);
  }

  public void writeLotClosedNoSale(Lot lot, String reason, Instant closedAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("lotId", lot.id());
    payload.put("sellerId", lot.sellerId());
    payload.put("reason", reason);
    enrichWithTraceContext(payload);
    outboxPort.append(
        OutboxEvent.AGGREGATE_LOT, lot.id(), OutboxEvent.LOT_CLOSED_NO_SALE, payload, closedAt);
  }

  private void enrichWithTraceContext(Map<String, Object> payload) {
    String traceId = MDC.get(TraceContext.TRACE_ID_MDC);
    if (traceId != null && !traceId.isBlank()) {
      payload.put("_traceId", traceId);
    }
    String spanId = MDC.get(TraceContext.SPAN_ID_MDC);
    if (spanId != null && !spanId.isBlank()) {
      payload.put("_spanId", spanId);
    }
  }
}
