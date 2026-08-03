package com.bidstream.application.lot;

import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.user.Role;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelLotUseCase {

  private final LotService lotService;

  public CancelLotUseCase(LotService lotService) {
    this.lotService = lotService;
  }

  @Transactional
  public Lot execute(long userId, Set<Role> roles, long lotId) {
    return lotService.cancelLot(userId, roles, lotId);
  }
}
