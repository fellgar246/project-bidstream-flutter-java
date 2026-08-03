package com.bidstream.application.lot;

import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.user.Role;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmImageUseCase {

  private final ImageConfirmService imageConfirmService;

  public ConfirmImageUseCase(ImageConfirmService imageConfirmService) {
    this.imageConfirmService = imageConfirmService;
  }

  @Transactional
  @PreAuthorize("hasRole('SELLER')")
  public LotImage execute(long userId, Set<Role> roles, long lotId, long imageId) {
    return imageConfirmService.confirm(userId, roles, lotId, imageId);
  }
}
