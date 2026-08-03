package com.bidstream.application.lot;

import com.bidstream.application.cache.CachedLotSnapshot;
import com.bidstream.application.cache.LotReadCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetLotUseCase {

  private final LotReadCacheService lotReadCacheService;

  public GetLotUseCase(LotReadCacheService lotReadCacheService) {
    this.lotReadCacheService = lotReadCacheService;
  }

  @Transactional(readOnly = true)
  public CachedLotSnapshot execute(long lotId) {
    return lotReadCacheService.getLotSnapshot(lotId);
  }
}
