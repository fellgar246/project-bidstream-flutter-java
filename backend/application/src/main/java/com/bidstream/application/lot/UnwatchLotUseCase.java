package com.bidstream.application.lot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnwatchLotUseCase {

  private final LotService lotService;

  public UnwatchLotUseCase(LotService lotService) {
    this.lotService = lotService;
  }

  @Transactional
  public void execute(long userId, long lotId) {
    lotService.unwatchLot(userId, lotId);
  }
}
