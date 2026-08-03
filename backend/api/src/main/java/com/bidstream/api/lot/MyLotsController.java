package com.bidstream.api.lot;

import com.bidstream.application.lot.ListMyLotsUseCase;
import com.bidstream.domain.user.Role;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MyLotsController {

  private final ListMyLotsUseCase listMyLotsUseCase;
  private final LotResponseMapper lotResponseMapper;

  public MyLotsController(
      ListMyLotsUseCase listMyLotsUseCase, LotResponseMapper lotResponseMapper) {
    this.listMyLotsUseCase = listMyLotsUseCase;
    this.lotResponseMapper = lotResponseMapper;
  }

  @GetMapping("/lots")
  public List<LotResponse> listMyLots(Authentication authentication) {
    long sellerId = (long) authentication.getPrincipal();
    Set<Role> roles = roles(authentication);
    return listMyLotsUseCase.execute(sellerId).stream()
        .map(lot -> lotResponseMapper.toResponse(lot, sellerId, roles))
        .toList();
  }

  private static Set<Role> roles(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(auth -> Role.valueOf(auth.replace("ROLE_", "")))
        .collect(Collectors.toSet());
  }
}
