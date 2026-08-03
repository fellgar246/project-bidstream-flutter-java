package com.bidstream.application.bid;

import com.bidstream.domain.money.Money;
import org.springframework.stereotype.Service;

@Service
public class PlaceBidUseCase {

  private final PlaceBidService placeBidService;

  public PlaceBidUseCase(PlaceBidService placeBidService) {
    this.placeBidService = placeBidService;
  }

  public PlaceBidService.PlaceBidOutcome execute(
      long lotId, long bidderId, Money amount, String clientRequestId) {
    return placeBidService.placeBid(lotId, bidderId, amount, clientRequestId);
  }
}
