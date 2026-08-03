package com.bidstream.api.lot;

import com.bidstream.application.lot.ListWatchlistUseCase;
import com.bidstream.application.lot.UnwatchLotUseCase;
import com.bidstream.application.lot.WatchLotUseCase;
import com.bidstream.domain.lot.LotPage;
import com.bidstream.domain.user.Role;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WatchController {

  private final WatchLotUseCase watchLotUseCase;
  private final UnwatchLotUseCase unwatchLotUseCase;
  private final ListWatchlistUseCase listWatchlistUseCase;
  private final LotResponseMapper lotResponseMapper;

  public WatchController(
      WatchLotUseCase watchLotUseCase,
      UnwatchLotUseCase unwatchLotUseCase,
      ListWatchlistUseCase listWatchlistUseCase,
      LotResponseMapper lotResponseMapper) {
    this.watchLotUseCase = watchLotUseCase;
    this.unwatchLotUseCase = unwatchLotUseCase;
    this.listWatchlistUseCase = listWatchlistUseCase;
    this.lotResponseMapper = lotResponseMapper;
  }

  @PostMapping("/lots/{id}/watch")
  public void watch(Authentication authentication, @PathVariable long id) {
    watchLotUseCase.execute(userId(authentication), id);
  }

  @DeleteMapping("/lots/{id}/watch")
  public void unwatch(Authentication authentication, @PathVariable long id) {
    unwatchLotUseCase.execute(userId(authentication), id);
  }

  @GetMapping("/me/watchlist")
  public LotPageResponse watchlist(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    long userId = userId(authentication);
    Set<Role> roles = roles(authentication);
    LotPage lotPage = listWatchlistUseCase.execute(userId, page, size);
    List<LotResponse> content =
        lotPage.content().stream()
            .map(lot -> lotResponseMapper.toResponse(lot, userId, roles))
            .toList();
    return new LotPageResponse(
        content,
        new LotPageResponse.PageMetadata(
            lotPage.page(), lotPage.size(), lotPage.totalElements(), lotPage.totalPages()));
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
