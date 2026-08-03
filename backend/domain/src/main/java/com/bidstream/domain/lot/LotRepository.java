package com.bidstream.domain.lot;

import java.util.List;
import java.util.Optional;

public interface LotRepository {

  Lot save(Lot lot);

  Optional<Lot> findById(long id);

  void deleteById(long id);

  LotPage findPublic(LotQuery query);

  List<Lot> findBySellerId(long sellerId);

  long countBySellerId(long sellerId);

  boolean hasReadyImages(long lotId);
}
