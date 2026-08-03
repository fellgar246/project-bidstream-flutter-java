package com.bidstream.application.lot;

import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.user.Role;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleLotUseCase {

  private final LotService lotService;

  public ScheduleLotUseCase(LotService lotService) {
    this.lotService = lotService;
  }

  @Transactional
  public Lot execute(
      long userId, Set<Role> roles, long lotId, Instant scheduledStartAt, Instant scheduledEndAt) {
    return lotService.scheduleLot(userId, roles, lotId, scheduledStartAt, scheduledEndAt);
  }
}
