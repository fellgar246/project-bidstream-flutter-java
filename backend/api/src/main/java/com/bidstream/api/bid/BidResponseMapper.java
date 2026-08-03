package com.bidstream.api.bid;

import com.bidstream.application.bid.BidQueryService;
import com.bidstream.application.bid.PlaceBidResult;
import com.bidstream.domain.bid.Bid;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.user.UserRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Component;

@Component
public class BidResponseMapper {

  private final UserRepository userRepository;

  public BidResponseMapper(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public PlaceBidResponse toPlaceBidResponse(PlaceBidResult result) {
    Bid bid = result.bid();
    Lot lot = result.lot();
    String displayName =
        userRepository
            .findById(bid.bidderId())
            .map(user -> user.displayName())
            .orElse("Bidder");
    return new PlaceBidResponse(
        new BidSummary(
            bid.id(), bid.amount().toString(), bid.placedAt().toString(), displayName),
        new PlaceBidLotSummary(
            lot.currentPrice().toString(),
            lot.bidCount(),
            lot.scheduledEndAt() != null ? lot.scheduledEndAt().toString() : null,
            result.extended()),
        result.youAreHighestBidder());
  }

  public BidSummary toSummary(Bid bid) {
    String displayName =
        userRepository
            .findById(bid.bidderId())
            .map(user -> user.displayName())
            .orElse("Bidder");
    return new BidSummary(
        bid.id(), bid.amount().toString(), bid.placedAt().toString(), displayName);
  }

  public MyBidResponse toMyBidResponse(BidQueryService.MyBidView view) {
    Bid bid = view.bid();
    Lot lot = view.lot();
    return new MyBidResponse(
        toSummary(bid),
        lot.id(),
        lot.title(),
        lot.status().name(),
        lot.currentPrice().toString(),
        lot.scheduledEndAt() != null ? lot.scheduledEndAt().toString() : null);
  }
}
