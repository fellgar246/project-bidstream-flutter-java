package com.bidstream.application.lot;

import com.bidstream.domain.lot.LotPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListWatchlistUseCase {

  private final LotService lotService;

  public ListWatchlistUseCase(LotService lotService) {
    this.lotService = lotService;
  }

  @Transactional(readOnly = true)
  public LotPage execute(long userId, int page, int size) {
    return lotService.listWatchlist(userId, page, size);
  }
}
