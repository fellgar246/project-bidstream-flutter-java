package com.bidstream.domain.lot;

import com.bidstream.domain.money.Money;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record Lot(
    long id,
    long sellerId,
    String title,
    String description,
    long categoryId,
    Money startingPrice,
    Money minIncrement,
    Money reservePrice,
    LotStatus status,
    Instant scheduledStartAt,
    Instant scheduledEndAt,
    Instant actualEndAt,
    Money currentPrice,
    int bidCount,
    Long winningBidId,
    int extensionCount,
    long version,
    Instant createdAt,
    Instant updatedAt) {

  private static final Duration MIN_DURATION = Duration.ofMinutes(5);
  private static final Duration MAX_DURATION = Duration.ofDays(14);
  private static final Duration MIN_SCHEDULE_LEAD = Duration.ofMinutes(5);

  public Lot {
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(startingPrice, "startingPrice");
    Objects.requireNonNull(minIncrement, "minIncrement");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(currentPrice, "currentPrice");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
    validatePrices(startingPrice, minIncrement, reservePrice);
  }

  public static Lot createDraft(
      long sellerId,
      String title,
      String description,
      long categoryId,
      Money startingPrice,
      Money minIncrement,
      Money reservePrice,
      Instant now) {
    validatePrices(startingPrice, minIncrement, reservePrice);
    return new Lot(
        0L,
        sellerId,
        title,
        description,
        categoryId,
        startingPrice,
        minIncrement,
        reservePrice,
        LotStatus.DRAFT,
        null,
        null,
        null,
        Money.fromCents(0),
        0,
        null,
        0,
        0L,
        now,
        now);
  }

  public Optional<Money> optionalReservePrice() {
    return reservePrice == null ? Optional.empty() : Optional.of(reservePrice);
  }

  public boolean hasReserve() {
    return reservePrice != null;
  }

  public boolean isReserveMet() {
    if (reservePrice == null) {
      return true;
    }
    return currentPrice.compareTo(reservePrice) >= 0;
  }

  public boolean isEditable() {
    return status == LotStatus.DRAFT || status == LotStatus.SCHEDULED;
  }

  public Lot updateDraft(
      String newTitle,
      String newDescription,
      long newCategoryId,
      Money newStartingPrice,
      Money newMinIncrement,
      Money newReservePrice,
      Instant now) {
    if (!isEditable()) {
      throw new InvalidTransitionException(status, LotEvent.UPDATE);
    }
    validatePrices(newStartingPrice, newMinIncrement, newReservePrice);
    return new Lot(
        id,
        sellerId,
        newTitle,
        newDescription,
        newCategoryId,
        newStartingPrice,
        newMinIncrement,
        newReservePrice,
        status,
        scheduledStartAt,
        scheduledEndAt,
        actualEndAt,
        currentPrice,
        bidCount,
        winningBidId,
        extensionCount,
        version,
        createdAt,
        now);
  }

  public Lot schedule(Instant startAt, Instant endAt, Instant now) {
    LotStatus next = LotStateMachine.transition(status, LotEvent.SCHEDULE);
    validateScheduleWindow(startAt, endAt, now);
    return new Lot(
        id,
        sellerId,
        title,
        description,
        categoryId,
        startingPrice,
        minIncrement,
        reservePrice,
        next,
        startAt,
        endAt,
        actualEndAt,
        currentPrice,
        bidCount,
        winningBidId,
        extensionCount,
        version,
        createdAt,
        now);
  }

  public Lot cancel(Instant now) {
    LotStatus next = LotStateMachine.transition(status, LotEvent.CANCEL);
    return new Lot(
        id,
        sellerId,
        title,
        description,
        categoryId,
        startingPrice,
        minIncrement,
        reservePrice,
        next,
        scheduledStartAt,
        scheduledEndAt,
        actualEndAt,
        currentPrice,
        bidCount,
        winningBidId,
        extensionCount,
        version,
        createdAt,
        now);
  }

  public Lot start(Instant now) {
    LotStatus next = LotStateMachine.transition(status, LotEvent.START);
    return new Lot(
        id,
        sellerId,
        title,
        description,
        categoryId,
        startingPrice,
        minIncrement,
        reservePrice,
        next,
        scheduledStartAt,
        scheduledEndAt,
        actualEndAt,
        currentPrice,
        bidCount,
        winningBidId,
        extensionCount,
        version,
        createdAt,
        now);
  }

  public Lot closeSold(long winningBidId, Instant now) {
    LotStatus next = LotStateMachine.transition(status, LotEvent.CLOSE_SOLD);
    return new Lot(
        id,
        sellerId,
        title,
        description,
        categoryId,
        startingPrice,
        minIncrement,
        reservePrice,
        next,
        scheduledStartAt,
        scheduledEndAt,
        now,
        currentPrice,
        bidCount,
        winningBidId,
        extensionCount,
        version,
        createdAt,
        now);
  }

  public Lot closeNoSale(Instant now) {
    LotStatus next = LotStateMachine.transition(status, LotEvent.CLOSE_NO_SALE);
    return new Lot(
        id,
        sellerId,
        title,
        description,
        categoryId,
        startingPrice,
        minIncrement,
        reservePrice,
        next,
        scheduledStartAt,
        scheduledEndAt,
        now,
        currentPrice,
        bidCount,
        winningBidId,
        extensionCount,
        version,
        createdAt,
        now);
  }

  public Lot withId(long newId) {
    return new Lot(
        newId,
        sellerId,
        title,
        description,
        categoryId,
        startingPrice,
        minIncrement,
        reservePrice,
        status,
        scheduledStartAt,
        scheduledEndAt,
        actualEndAt,
        currentPrice,
        bidCount,
        winningBidId,
        extensionCount,
        version,
        createdAt,
        updatedAt);
  }

  /** Minimum valid bid amount for RB-04. */
  public Money minimumNextBid() {
    if (bidCount == 0) {
      return startingPrice;
    }
    return currentPrice.add(minIncrement);
  }

  /** Applies an accepted bid and anti-sniping extension (RB-06). */
  public BidAcceptanceResult acceptBid(Money amount, Instant now) {
    Duration timeToEnd = Duration.between(now, scheduledEndAt);
    boolean extended = false;
    Instant newEndAt = scheduledEndAt;
    int newExtensionCount = extensionCount;

    if (scheduledEndAt != null
        && timeToEnd.compareTo(Duration.ofSeconds(30)) < 0
        && extensionCount < 10) {
      newEndAt = now.plus(Duration.ofSeconds(30));
      newExtensionCount = extensionCount + 1;
      extended = true;
    }

    Lot updated =
        new Lot(
            id,
            sellerId,
            title,
            description,
            categoryId,
            startingPrice,
            minIncrement,
            reservePrice,
            status,
            scheduledStartAt,
            newEndAt,
            actualEndAt,
            amount,
            bidCount + 1,
            winningBidId,
            newExtensionCount,
            version,
            createdAt,
            now);
    return new BidAcceptanceResult(updated, extended);
  }

  public record BidAcceptanceResult(Lot lot, boolean extended) {}

  public Lot withVersion(long newVersion) {
    return new Lot(
        id,
        sellerId,
        title,
        description,
        categoryId,
        startingPrice,
        minIncrement,
        reservePrice,
        status,
        scheduledStartAt,
        scheduledEndAt,
        actualEndAt,
        currentPrice,
        bidCount,
        winningBidId,
        extensionCount,
        newVersion,
        createdAt,
        updatedAt);
  }

  public Lot forceStatus(LotStatus newStatus, Instant now) {
    return new Lot(
        id,
        sellerId,
        title,
        description,
        categoryId,
        startingPrice,
        minIncrement,
        reservePrice,
        newStatus,
        scheduledStartAt,
        scheduledEndAt,
        actualEndAt,
        currentPrice,
        bidCount,
        winningBidId,
        extensionCount,
        version,
        createdAt,
        now);
  }

  public static void validatePrices(Money startingPrice, Money minIncrement, Money reservePrice) {
    if (startingPrice.cents() <= 0) {
      throw new IllegalArgumentException("starting price must be positive");
    }
    if (minIncrement.cents() <= 0) {
      throw new IllegalArgumentException("min increment must be positive");
    }
    if (reservePrice != null && reservePrice.compareTo(startingPrice) < 0) {
      throw new IllegalArgumentException("reserve price must be >= starting price");
    }
  }

  public static void validateScheduleWindow(Instant startAt, Instant endAt, Instant now) {
    if (startAt == null || endAt == null) {
      throw new LotValidationException(
          "Schedule window is invalid", Map.of("scheduledStartAt", "must not be null"));
    }
    if (!endAt.isAfter(startAt)) {
      throw new LotValidationException(
          "Schedule window is invalid", Map.of("scheduledEndAt", "must be after scheduledStartAt"));
    }
    Duration duration = Duration.between(startAt, endAt);
    if (duration.compareTo(MIN_DURATION) < 0 || duration.compareTo(MAX_DURATION) > 0) {
      throw new LotValidationException(
          "Schedule window is invalid",
          Map.of("scheduledEndAt", "duration must be between 5 minutes and 14 days"));
    }
    if (startAt.isBefore(now.plus(MIN_SCHEDULE_LEAD))) {
      throw new LotValidationException(
          "Schedule window is invalid",
          Map.of("scheduledStartAt", "must be at least 5 minutes in the future"));
    }
  }
}
