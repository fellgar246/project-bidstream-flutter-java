package com.bidstream.application.lot;

import com.bidstream.domain.user.Role;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresignImageUseCase {

  private final PresignService presignService;

  public PresignImageUseCase(PresignService presignService) {
    this.presignService = presignService;
  }

  @Transactional
  @PreAuthorize("hasRole('SELLER')")
  public PresignService.PresignResult execute(
      long userId,
      Set<Role> roles,
      long lotId,
      String fileName,
      String contentType,
      long sizeBytes) {
    return presignService.presign(userId, roles, lotId, fileName, contentType, sizeBytes);
  }
}
