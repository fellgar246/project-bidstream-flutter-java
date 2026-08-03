package com.bidstream.application.cache;

import com.bidstream.application.lot.LotService;
import com.bidstream.domain.lot.Lot;
import org.springframework.stereotype.Component;

@Component
public class LotCacheLoader {

  private final LotService lotService;

  public LotCacheLoader(LotService lotService) {
    this.lotService = lotService;
  }

  public Lot loadLot(long lotId) {
    return lotService.getLot(lotId);
  }
}
