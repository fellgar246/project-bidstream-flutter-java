package com.bidstream.application.bid;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListMyBidsUseCase {

  private final BidQueryService bidQueryService;

  public ListMyBidsUseCase(BidQueryService bidQueryService) {
    this.bidQueryService = bidQueryService;
  }

  @Transactional(readOnly = true)
  public List<BidQueryService.MyBidView> execute(long bidderId, int page, int size) {
    return bidQueryService.listMyBidsWithLotStatus(bidderId, page, size);
  }
}
