package com.bidstream.application.lot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WatchLotUseCase {

  private final LotService lotService;

  public WatchLotUseCase(LotService lotService) {
    this.lotService = lotService;
  }

  @Transactional
  public void execute(long userId, long lotId) {
    lotService.watchLot(userId, lotId);
  }
}
