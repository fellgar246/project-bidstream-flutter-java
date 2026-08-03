package com.bidstream.application.bid;

import com.bidstream.domain.bid.BidPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListLotBidsUseCase {

  private final BidQueryService bidQueryService;

  public ListLotBidsUseCase(BidQueryService bidQueryService) {
    this.bidQueryService = bidQueryService;
  }

  @Transactional(readOnly = true)
  public BidPage execute(long lotId, int page, int size) {
    return bidQueryService.listLotBids(lotId, page, size);
  }
}
