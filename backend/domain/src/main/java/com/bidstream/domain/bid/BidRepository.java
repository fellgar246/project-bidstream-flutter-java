package com.bidstream.domain.bid;

import java.util.List;
import java.util.Optional;

public interface BidRepository {

  Bid save(Bid bid);

  Optional<Bid> findByLotIdAndBidderIdAndClientRequestId(
      long lotId, long bidderId, String clientRequestId);

  Optional<Bid> findHighestBidByLotId(long lotId);

  List<Bid> findByLotIdAfterBidId(long lotId, long afterBidId, int limit);

  BidPage findByLotId(long lotId, int page, int size);

  BidPage findByBidderId(long bidderId, int page, int size);

  long countByLotId(long lotId);
}
