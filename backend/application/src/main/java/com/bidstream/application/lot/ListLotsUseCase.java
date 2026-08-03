package com.bidstream.application.lot;

import com.bidstream.domain.lot.LotPage;
import com.bidstream.domain.lot.LotQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListLotsUseCase {

  private final LotService lotService;

  public ListLotsUseCase(LotService lotService) {
    this.lotService = lotService;
  }

  @Transactional(readOnly = true)
  public LotPage execute(LotQuery query) {
    return lotService.listPublicLots(query);
  }
}
