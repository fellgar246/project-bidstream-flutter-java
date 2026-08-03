package com.bidstream.application.lot;

import com.bidstream.domain.user.Role;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteLotUseCase {

  private final LotService lotService;

  public DeleteLotUseCase(LotService lotService) {
    this.lotService = lotService;
  }

  @Transactional
  public void execute(long userId, Set<Role> roles, long lotId) {
    lotService.deleteLot(userId, roles, lotId);
  }
}
