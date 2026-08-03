package com.bidstream.domain.lot;

import java.util.List;
import java.util.Optional;

public interface WatchRepository {

  Watch save(Watch watch);

  Optional<Watch> findByUserIdAndLotId(long userId, long lotId);

  void deleteByUserIdAndLotId(long userId, long lotId);

  List<Watch> findByUserId(long userId, int page, int size);

  long countByUserId(long userId);

  List<Long> findUserIdsByLotId(long lotId);
}
