package com.bidstream.application.lot;

import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.Role;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateLotUseCase {

  private final LotService lotService;

  public UpdateLotUseCase(LotService lotService) {
    this.lotService = lotService;
  }

  @Transactional
  public Lot execute(
      long userId,
      Set<Role> roles,
      long lotId,
      String title,
      String description,
      long categoryId,
      Money startingPrice,
      Money minIncrement,
      Money reservePrice) {
    return lotService.updateLot(
        userId,
        roles,
        lotId,
        title,
        description,
        categoryId,
        startingPrice,
        minIncrement,
        reservePrice);
  }
}
