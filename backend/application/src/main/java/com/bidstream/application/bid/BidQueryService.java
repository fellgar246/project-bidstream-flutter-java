package com.bidstream.application.bid;

import com.bidstream.domain.bid.BidPage;
import com.bidstream.domain.bid.BidRepository;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.user.User;
import com.bidstream.domain.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class BidQueryService {

  private final BidRepository bidRepository;
  private final LotRepository lotRepository;
  private final UserRepository userRepository;

  public BidQueryService(
      BidRepository bidRepository, LotRepository lotRepository, UserRepository userRepository) {
    this.bidRepository = bidRepository;
    this.lotRepository = lotRepository;
    this.userRepository = userRepository;
  }

  public BidPage listLotBids(long lotId, int page, int size) {
    lotRepository.findById(lotId).orElseThrow(() -> new NoSuchElementException("Lot not found"));
    return bidRepository.findByLotId(lotId, page, size);
  }

  public BidPage listMyBids(long bidderId, int page, int size) {
    return bidRepository.findByBidderId(bidderId, page, size);
  }

  public List<MyBidView> listMyBidsWithLotStatus(long bidderId, int page, int size) {
    BidPage bids = bidRepository.findByBidderId(bidderId, page, size);
    List<MyBidView> views = new ArrayList<>();
    for (var bid : bids.content()) {
      Lot lot =
          lotRepository
              .findById(bid.lotId())
              .orElseThrow(() -> new NoSuchElementException("Lot not found"));
      views.add(new MyBidView(bid, lot));
    }
    return views;
  }

  public String bidderDisplayName(long bidderId) {
    return userRepository
        .findById(bidderId)
        .map(User::displayName)
        .orElse("Bidder");
  }

  public record MyBidView(com.bidstream.domain.bid.Bid bid, Lot lot) {}
}
