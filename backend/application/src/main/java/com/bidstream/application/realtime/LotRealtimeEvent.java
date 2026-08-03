package com.bidstream.application.realtime;

import com.bidstream.domain.bid.Bid;
import com.bidstream.domain.lot.Lot;
import java.time.Instant;
import java.util.Optional;

/** Application events emitted after a successful bid (published inside the transaction). */
public sealed interface LotRealtimeEvent
    permits LotRealtimeEvent.BidPlacedEvent, LotRealtimeEvent.LotExtendedEvent {

  long lotId();

  Instant occurredAt();

  record BidPlacedEvent(
      long lotId,
      Bid bid,
      Lot lot,
      String bidderDisplayName,
      boolean extended,
      Optional<Long> previousHighestBidderId,
      Optional<com.bidstream.domain.money.Money> previousHighestAmount,
      Instant occurredAt)
      implements LotRealtimeEvent {}

  record LotExtendedEvent(long lotId, Lot lot, long bidId, Instant occurredAt)
      implements LotRealtimeEvent {}
}
