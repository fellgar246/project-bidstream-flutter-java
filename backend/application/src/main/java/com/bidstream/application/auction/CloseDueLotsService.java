package com.bidstream.application.auction;

import com.bidstream.domain.lot.LotRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CloseDueLotsService {

  private static final int BATCH_SIZE = 50;

  private final LotRepository lotRepository;
  private final CloseAuctionService closeAuctionService;
  private final Clock clock;

  public CloseDueLotsService(
      LotRepository lotRepository, CloseAuctionService closeAuctionService, Clock clock) {
    this.lotRepository = lotRepository;
    this.closeAuctionService = closeAuctionService;
    this.clock = clock;
  }

  public int closeDueLots() {
    List<Long> dueIds = lotRepository.findLiveIdsReadyToClose(clock.instant(), BATCH_SIZE);
    int closed = 0;
    for (long lotId : dueIds) {
      closeAuctionService.closeLot(lotId);
      closed++;
    }
    return closed;
  }
}
