package com.bidstream.application.realtime;

import com.bidstream.domain.bid.Bid;
import com.bidstream.domain.bid.BidRepository;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.user.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class ListLotEventsUseCase {

  private final LotRepository lotRepository;
  private final BidRepository bidRepository;
  private final UserRepository userRepository;

  public ListLotEventsUseCase(
      LotRepository lotRepository, BidRepository bidRepository, UserRepository userRepository) {
    this.lotRepository = lotRepository;
    this.bidRepository = bidRepository;
    this.userRepository = userRepository;
  }

  public List<LotEventEnvelope> execute(long lotId, long afterEventId, int limit) {
    lotRepository.findById(lotId).orElseThrow(() -> new NoSuchElementException("Lot not found"));
    long afterBidId = afterEventId / 2L;
    List<Bid> bids = bidRepository.findByLotIdAfterBidId(lotId, afterBidId, limit);
    List<LotEventEnvelope> events = new ArrayList<>(bids.size());
    for (Bid bid : bids) {
      String displayName =
          userRepository.findById(bid.bidderId()).map(user -> user.displayName()).orElse("Bidder");
      Lot lot =
          lotRepository
              .findById(lotId)
              .orElseThrow(() -> new NoSuchElementException("Lot not found"));
      events.add(toBidPlacedEnvelope(bid, lot, displayName));
    }
    return events;
  }

  static LotEventEnvelope toBidPlacedEnvelope(Bid bid, Lot lot, String bidderDisplayName) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("bidId", bid.id());
    payload.put("amount", bid.amount().toString());
    payload.put("bidderDisplayName", bidderDisplayName);
    payload.put("currentPrice", lot.currentPrice().toString());
    payload.put("bidCount", lot.bidCount());
    payload.put(
        "scheduledEndAt", lot.scheduledEndAt() != null ? lot.scheduledEndAt().toString() : null);
    payload.put("extended", false);
    return new LotEventEnvelope(
        LotEventEnvelope.eventIdForBid(bid.id()),
        LotEventEnvelope.BID_PLACED,
        bid.lotId(),
        bid.placedAt(),
        payload);
  }

  static LotEventEnvelope toExtendedEnvelope(Bid bid, Lot lot) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put(
        "scheduledEndAt", lot.scheduledEndAt() != null ? lot.scheduledEndAt().toString() : null);
    return new LotEventEnvelope(
        LotEventEnvelope.eventIdForExtension(bid.id()),
        LotEventEnvelope.LOT_EXTENDED,
        lot.id(),
        bid.placedAt(),
        payload);
  }
}
