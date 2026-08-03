package com.bidstream.api.lot;

import com.bidstream.application.lot.CancelLotUseCase;
import com.bidstream.application.lot.CreateLotUseCase;
import com.bidstream.application.lot.DeleteLotUseCase;
import com.bidstream.application.lot.GetLotUseCase;
import com.bidstream.application.lot.ListLotsUseCase;
import com.bidstream.application.lot.ScheduleLotUseCase;
import com.bidstream.application.lot.UpdateLotUseCase;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotPage;
import com.bidstream.domain.lot.LotQuery;
import com.bidstream.domain.lot.LotSort;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.Role;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lots")
public class LotController {

  private final CreateLotUseCase createLotUseCase;
  private final UpdateLotUseCase updateLotUseCase;
  private final DeleteLotUseCase deleteLotUseCase;
  private final ScheduleLotUseCase scheduleLotUseCase;
  private final CancelLotUseCase cancelLotUseCase;
  private final ListLotsUseCase listLotsUseCase;
  private final GetLotUseCase getLotUseCase;
  private final LotResponseMapper lotResponseMapper;

  public LotController(
      CreateLotUseCase createLotUseCase,
      UpdateLotUseCase updateLotUseCase,
      DeleteLotUseCase deleteLotUseCase,
      ScheduleLotUseCase scheduleLotUseCase,
      CancelLotUseCase cancelLotUseCase,
      ListLotsUseCase listLotsUseCase,
      GetLotUseCase getLotUseCase,
      LotResponseMapper lotResponseMapper) {
    this.createLotUseCase = createLotUseCase;
    this.updateLotUseCase = updateLotUseCase;
    this.deleteLotUseCase = deleteLotUseCase;
    this.scheduleLotUseCase = scheduleLotUseCase;
    this.cancelLotUseCase = cancelLotUseCase;
    this.listLotsUseCase = listLotsUseCase;
    this.getLotUseCase = getLotUseCase;
    this.lotResponseMapper = lotResponseMapper;
  }

  @PostMapping
  public ResponseEntity<LotResponse> create(
      Authentication authentication, @Valid @RequestBody CreateLotRequest request) {
    long sellerId = userId(authentication);
    Lot lot =
        createLotUseCase.execute(
            sellerId,
            request.title(),
            request.description(),
            request.categoryId(),
            Money.fromString(request.startingPrice()),
            Money.fromString(request.minIncrement()),
            parseOptionalMoney(request.reservePrice()));
    LotResponse response = lotResponseMapper.toResponse(lot, sellerId, roles(authentication));
    return ResponseEntity.created(URI.create("/api/v1/lots/" + lot.id())).body(response);
  }

  @PatchMapping("/{id}")
  public LotResponse update(
      Authentication authentication,
      @PathVariable long id,
      @Valid @RequestBody UpdateLotRequest request) {
    Lot lot =
        updateLotUseCase.execute(
            userId(authentication),
            roles(authentication),
            id,
            request.title(),
            request.description(),
            request.categoryId(),
            Money.fromString(request.startingPrice()),
            Money.fromString(request.minIncrement()),
            parseOptionalMoney(request.reservePrice()));
    return lotResponseMapper.toResponse(lot, userId(authentication), roles(authentication));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(Authentication authentication, @PathVariable long id) {
    deleteLotUseCase.execute(userId(authentication), roles(authentication), id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/schedule")
  public LotResponse schedule(
      Authentication authentication,
      @PathVariable long id,
      @Valid @RequestBody ScheduleLotRequest request) {
    Lot lot =
        scheduleLotUseCase.execute(
            userId(authentication),
            roles(authentication),
            id,
            request.scheduledStartAt(),
            request.scheduledEndAt());
    return lotResponseMapper.toResponse(lot, userId(authentication), roles(authentication));
  }

  @PostMapping("/{id}/cancel")
  public LotResponse cancel(Authentication authentication, @PathVariable long id) {
    Lot lot = cancelLotUseCase.execute(userId(authentication), roles(authentication), id);
    return lotResponseMapper.toResponse(lot, userId(authentication), roles(authentication));
  }

  @GetMapping
  public LotPageResponse list(
      Authentication authentication,
      @RequestParam(required = false) LotStatus status,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long minPriceCents,
      @RequestParam(required = false) Long maxPriceCents,
      @RequestParam(required = false) Long sellerId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "endingSoon") String sort) {
    LotQuery query =
        new LotQuery(
            Optional.ofNullable(status),
            Optional.ofNullable(categoryId),
            Optional.ofNullable(minPriceCents),
            Optional.ofNullable(maxPriceCents),
            Optional.ofNullable(sellerId),
            Optional.ofNullable(q),
            page,
            size,
            parseSort(sort));
    LotPage lotPage = listLotsUseCase.execute(query);
    Long viewerUserId = authentication != null ? userId(authentication) : null;
    Set<Role> viewerRoles = authentication != null ? roles(authentication) : Set.of();
    List<LotResponse> content =
        lotPage.content().stream()
            .map(lot -> lotResponseMapper.toResponse(lot, viewerUserId, viewerRoles))
            .toList();
    return new LotPageResponse(
        content,
        new LotPageResponse.PageMetadata(
            lotPage.page(), lotPage.size(), lotPage.totalElements(), lotPage.totalPages()));
  }

  @GetMapping("/{id}")
  public LotResponse get(Authentication authentication, @PathVariable long id) {
    Lot lot = getLotUseCase.execute(id);
    Long viewerUserId = authentication != null ? userId(authentication) : null;
    Set<Role> viewerRoles = authentication != null ? roles(authentication) : Set.of();
    return lotResponseMapper.toResponse(lot, viewerUserId, viewerRoles);
  }

  private static LotSort parseSort(String sort) {
    return switch (sort) {
      case "newest" -> LotSort.NEWEST;
      case "priceAsc" -> LotSort.PRICE_ASC;
      case "priceDesc" -> LotSort.PRICE_DESC;
      default -> LotSort.ENDING_SOON;
    };
  }

  private static Money parseOptionalMoney(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Money.fromString(value);
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
