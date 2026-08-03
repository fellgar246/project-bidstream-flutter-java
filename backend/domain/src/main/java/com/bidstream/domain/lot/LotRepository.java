package com.bidstream.domain.lot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LotRepository {

  Lot save(Lot lot);

  Optional<Lot> findById(long id);

  Optional<Lot> findByIdForUpdate(long id);

  void deleteById(long id);

  LotPage findPublic(LotQuery query);

  LotSearchResult search(LotQuery query);

  List<Lot> findBySellerId(long sellerId);

  long countBySellerId(long sellerId);

  boolean hasReadyImages(long lotId);

  List<Lot> findScheduledReadyToStart(Instant now, int limit);

  List<Long> findLiveIdsReadyToClose(Instant now, int limit);

  long countByStatus(LotStatus status);
}
