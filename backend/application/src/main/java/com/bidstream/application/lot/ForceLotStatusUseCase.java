package com.bidstream.application.lot;

import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForceLotStatusUseCase {

  private final LotService lotService;

  public ForceLotStatusUseCase(LotService lotService) {
    this.lotService = lotService;
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public Lot execute(long lotId, LotStatus status) {
    return lotService.forceStatus(lotId, status);
  }
}
