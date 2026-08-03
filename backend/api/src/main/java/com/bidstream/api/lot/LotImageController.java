package com.bidstream.api.lot;

import com.bidstream.application.lot.ConfirmImageUseCase;
import com.bidstream.application.lot.DeleteLotImageUseCase;
import com.bidstream.application.lot.PresignImageUseCase;
import com.bidstream.application.lot.PresignService;
import com.bidstream.application.lot.ReorderLotImagesUseCase;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.user.Role;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lots/{lotId}/images")
public class LotImageController {

  private final PresignImageUseCase presignImageUseCase;
  private final ConfirmImageUseCase confirmImageUseCase;
  private final DeleteLotImageUseCase deleteLotImageUseCase;
  private final ReorderLotImagesUseCase reorderLotImagesUseCase;
  private final LotImageResponseMapper lotImageResponseMapper;

  public LotImageController(
      PresignImageUseCase presignImageUseCase,
      ConfirmImageUseCase confirmImageUseCase,
      DeleteLotImageUseCase deleteLotImageUseCase,
      ReorderLotImagesUseCase reorderLotImagesUseCase,
      LotImageResponseMapper lotImageResponseMapper) {
    this.presignImageUseCase = presignImageUseCase;
    this.confirmImageUseCase = confirmImageUseCase;
    this.deleteLotImageUseCase = deleteLotImageUseCase;
    this.reorderLotImagesUseCase = reorderLotImagesUseCase;
    this.lotImageResponseMapper = lotImageResponseMapper;
  }

  @PostMapping("/presign")
  public ResponseEntity<PresignImageResponse> presign(
      Authentication authentication,
      @PathVariable long lotId,
      @Valid @RequestBody PresignImageRequest request) {
    PresignService.PresignResult result =
        presignImageUseCase.execute(
            userId(authentication),
            roles(authentication),
            lotId,
            request.fileName(),
            request.contentType(),
            request.sizeBytes());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new PresignImageResponse(
                result.imageId(), result.uploadUrl(), result.storageKey(), result.expiresIn()));
  }

  @PostMapping("/{imageId}/confirm")
  public Map<String, LotImageResponse> confirm(
      Authentication authentication, @PathVariable long lotId, @PathVariable long imageId) {
    LotImage image =
        confirmImageUseCase.execute(userId(authentication), roles(authentication), lotId, imageId);
    return Map.of("image", lotImageResponseMapper.toResponse(image));
  }

  @DeleteMapping("/{imageId}")
  public ResponseEntity<Void> delete(
      Authentication authentication, @PathVariable long lotId, @PathVariable long imageId) {
    deleteLotImageUseCase.execute(userId(authentication), roles(authentication), lotId, imageId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/order")
  public List<LotImageResponse> reorder(
      Authentication authentication,
      @PathVariable long lotId,
      @Valid @RequestBody ReorderImagesRequest request) {
    List<LotImage> images =
        reorderLotImagesUseCase.execute(
            userId(authentication), roles(authentication), lotId, request.order());
    return images.stream().map(lotImageResponseMapper::toResponse).toList();
  }

  private static long userId(Authentication authentication) {
    return (long) authentication.getPrincipal();
  }

  private static Set<Role> roles(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(auth -> Role.valueOf(auth.replace("ROLE_", "")))
        .collect(Collectors.toSet());
  }
}
