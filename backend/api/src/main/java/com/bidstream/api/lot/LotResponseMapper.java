package com.bidstream.api.lot;

import com.bidstream.application.lot.LotService;
import com.bidstream.domain.category.CategoryRepository;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.user.Role;
import com.bidstream.domain.user.UserRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LotResponseMapper {

  private final CategoryRepository categoryRepository;
  private final UserRepository userRepository;
  private final LotService lotService;

  public LotResponseMapper(
      CategoryRepository categoryRepository, UserRepository userRepository, LotService lotService) {
    this.categoryRepository = categoryRepository;
    this.userRepository = userRepository;
    this.lotService = lotService;
  }

  public LotResponse toResponse(Lot lot, Long viewerUserId, Set<Role> viewerRoles) {
    var category =
        categoryRepository
            .findById(lot.categoryId())
            .orElseThrow(() -> new NoSuchElementException("Category not found"));
    var seller =
        userRepository
            .findById(lot.sellerId())
            .orElseThrow(() -> new NoSuchElementException("Seller not found"));

    boolean watched = viewerUserId != null && lotService.isWatched(viewerUserId, lot.id());

    return new LotResponse(
        lot.id(),
        lot.title(),
        lot.description(),
        new CategorySummary(category.id(), category.name()),
        new SellerSummary(seller.id(), seller.displayName()),
        lot.startingPrice().toString(),
        lot.minIncrement().toString(),
        lot.currentPrice().toString(),
        lot.bidCount(),
        lot.hasReserve(),
        lot.isReserveMet(),
        lot.status().name(),
        lot.scheduledStartAt() != null ? lot.scheduledStartAt().toString() : null,
        lot.scheduledEndAt() != null ? lot.scheduledEndAt().toString() : null,
        lot.actualEndAt() != null ? lot.actualEndAt().toString() : null,
        List.of(),
        watched,
        lotService.canEdit(lot, viewerUserId, viewerRoles),
        lotService.canBid(lot, viewerUserId));
  }
}
