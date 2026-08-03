package com.bidstream.application.lot;

import com.bidstream.domain.lot.Lot;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListMyLotsUseCase {

  private final LotService lotService;

  public ListMyLotsUseCase(LotService lotService) {
    this.lotService = lotService;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('SELLER')")
  public List<Lot> execute(long sellerId) {
    return lotService.listSellerLots(sellerId);
  }
}
