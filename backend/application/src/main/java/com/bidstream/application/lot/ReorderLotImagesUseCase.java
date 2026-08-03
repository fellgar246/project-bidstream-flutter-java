package com.bidstream.application.lot;

import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.user.Role;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReorderLotImagesUseCase {

  private final LotImageService lotImageService;

  public ReorderLotImagesUseCase(LotImageService lotImageService) {
    this.lotImageService = lotImageService;
  }

  @Transactional
  @PreAuthorize("hasRole('SELLER')")
  public List<LotImage> execute(long userId, Set<Role> roles, long lotId, List<Long> order) {
    return lotImageService.reorderImages(userId, roles, lotId, order);
  }
}
