package com.bidstream.api.realtime;

import com.bidstream.application.realtime.LotEventEnvelope;
import com.bidstream.application.realtime.LotRealtimeEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LotEventMapper {

  public LotEventEnvelope toBidPlaced(LotRealtimeEvent.BidPlacedEvent event) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("bidId", event.bid().id());
    payload.put("amount", event.bid().amount().toString());
    payload.put("bidderDisplayName", event.bidderDisplayName());
    payload.put("currentPrice", event.lot().currentPrice().toString());
    payload.put("bidCount", event.lot().bidCount());
    payload.put(
        "scheduledEndAt",
        event.lot().scheduledEndAt() != null ? event.lot().scheduledEndAt().toString() : null);
    payload.put("extended", event.extended());
    return new LotEventEnvelope(
        LotEventEnvelope.eventIdForBid(event.bid().id()),
        LotEventEnvelope.BID_PLACED,
        event.lotId(),
        event.occurredAt(),
        payload);
  }

  public LotEventEnvelope toLotExtended(LotRealtimeEvent.LotExtendedEvent event) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put(
        "scheduledEndAt",
        event.lot().scheduledEndAt() != null ? event.lot().scheduledEndAt().toString() : null);
    return new LotEventEnvelope(
        LotEventEnvelope.eventIdForExtension(event.bidId()),
        LotEventEnvelope.LOT_EXTENDED,
        event.lotId(),
        event.occurredAt(),
        payload);
  }

  public LotEventEnvelope toLotStarted(LotRealtimeEvent.LotStartedEvent event) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("lotId", event.lotId());
    payload.put("sellerId", event.lot().sellerId());
    payload.put("status", event.lot().status().name());
    return new LotEventEnvelope(
        LotEventEnvelope.eventIdForLotLifecycle(event.lotId(), LotEventEnvelope.LOT_STARTED),
        LotEventEnvelope.LOT_STARTED,
        event.lotId(),
        event.occurredAt(),
        payload);
  }

  public LotEventEnvelope toLotClosed(LotRealtimeEvent.LotClosedEvent event) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("lotId", event.lotId());
    payload.put("status", event.lot().status().name());
    event.winnerId().ifPresent(winnerId -> payload.put("winnerId", winnerId));
    if (event.noSaleReason() != null) {
      payload.put("reason", event.noSaleReason());
    }
    if (event.lot().winningBidId() != null) {
      payload.put("winningBidId", event.lot().winningBidId());
    }
    payload.put("currentPrice", event.lot().currentPrice().toString());
    return new LotEventEnvelope(
        LotEventEnvelope.eventIdForLotLifecycle(event.lotId(), LotEventEnvelope.LOT_CLOSED),
        LotEventEnvelope.LOT_CLOSED,
        event.lotId(),
        event.occurredAt(),
        payload);
  }
}
