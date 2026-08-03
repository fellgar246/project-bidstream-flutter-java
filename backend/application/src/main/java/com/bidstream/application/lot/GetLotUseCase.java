package com.bidstream.application.lot;

import com.bidstream.domain.lot.Lot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetLotUseCase {

  private final LotService lotService;

  public GetLotUseCase(LotService lotService) {
    this.lotService = lotService;
  }

  @Transactional(readOnly = true)
  public Lot execute(long lotId) {
    return lotService.getLot(lotId);
  }
}
