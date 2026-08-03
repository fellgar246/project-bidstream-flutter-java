package com.bidstream.domain.bid;

import java.util.Optional;

public interface BidRepository {

  Bid save(Bid bid);

  Optional<Bid> findByLotIdAndBidderIdAndClientRequestId(
      long lotId, long bidderId, String clientRequestId);

  BidPage findByLotId(long lotId, int page, int size);

  BidPage findByBidderId(long bidderId, int page, int size);

  long countByLotId(long lotId);
}
