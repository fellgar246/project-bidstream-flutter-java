package com.bidstream.application.lot;

import com.bidstream.domain.lot.ForbiddenLotAccessException;
import com.bidstream.domain.lot.InvalidTransitionException;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotEvent;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.user.Role;
import java.util.Set;

final class LotAccessGuard {

  private LotAccessGuard() {}

  static void requireOwnerOrAdmin(Lot lot, long userId, Set<Role> roles) {
    if (roles.contains(Role.ADMIN)) {
      return;
    }
    if (lot.sellerId() != userId) {
      throw new ForbiddenLotAccessException("Not the lot owner");
    }
  }

  static void requireDeletable(Lot lot) {
    if (lot.status() != LotStatus.DRAFT) {
      throw new InvalidTransitionException(lot.status(), LotEvent.DELETE);
    }
  }
}
