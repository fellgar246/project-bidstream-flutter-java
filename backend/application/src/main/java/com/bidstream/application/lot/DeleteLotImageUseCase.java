package com.bidstream.application.lot;

import com.bidstream.domain.user.Role;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteLotImageUseCase {

  private final LotImageService lotImageService;

  public DeleteLotImageUseCase(LotImageService lotImageService) {
    this.lotImageService = lotImageService;
  }

  @Transactional
  @PreAuthorize("hasRole('SELLER')")
  public void execute(long userId, Set<Role> roles, long lotId, long imageId) {
    lotImageService.deleteImage(userId, roles, lotId, imageId);
  }
}
