package com.bidstream.api.lot;

import com.bidstream.application.lot.ForceLotStatusUseCase;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.user.Role;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/lots")
@Profile("dev")
public class AdminLotController {

  private final ForceLotStatusUseCase forceLotStatusUseCase;
  private final LotResponseMapper lotResponseMapper;

  public AdminLotController(
      ForceLotStatusUseCase forceLotStatusUseCase, LotResponseMapper lotResponseMapper) {
    this.forceLotStatusUseCase = forceLotStatusUseCase;
    this.lotResponseMapper = lotResponseMapper;
  }

  @PostMapping("/{id}/force-status")
  public LotResponse forceStatus(
      Authentication authentication,
      @PathVariable long id,
      @Valid @RequestBody ForceStatusRequest request) {
    Lot lot = forceLotStatusUseCase.execute(id, request.status());
    return lotResponseMapper.toResponse(lot, userId(authentication), roles(authentication));
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
