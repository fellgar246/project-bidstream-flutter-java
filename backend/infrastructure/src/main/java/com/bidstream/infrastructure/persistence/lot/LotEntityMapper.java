package com.bidstream.infrastructure.persistence.lot;

import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.money.Money;

final class LotEntityMapper {

  private LotEntityMapper() {}

  static Lot toDomain(LotEntity entity) {
    Money reservePrice =
        entity.getReservePriceCents() == null
            ? null
            : Money.fromCents(entity.getReservePriceCents());
    return new Lot(
        entity.getId(),
        entity.getSeller().getId(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getCategory().getId(),
        Money.fromCents(entity.getStartingPriceCents()),
        Money.fromCents(entity.getMinIncrementCents()),
        reservePrice,
        LotStatus.valueOf(entity.getStatus()),
        entity.getScheduledStartAt(),
        entity.getScheduledEndAt(),
        entity.getActualEndAt(),
        Money.fromCents(entity.getCurrentPriceCents()),
        entity.getBidCount(),
        entity.getWinningBidId(),
        entity.getExtensionCount(),
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
