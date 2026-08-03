package com.bidstream.domain.lot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LotImageRepository {

  LotImage save(LotImage image);

  Optional<LotImage> findById(long id);

  Optional<LotImage> findByIdAndLotId(long id, long lotId);

  List<LotImage> findByLotIdOrderByPosition(long lotId);

  int countByLotId(long lotId);

  boolean hasReadyImages(long lotId);

  void deleteById(long id);

  List<LotImage> findPendingOlderThan(Instant cutoff);
}
