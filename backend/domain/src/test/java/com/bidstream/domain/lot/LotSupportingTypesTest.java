package com.bidstream.domain.lot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidstream.domain.money.Money;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LotSupportingTypesTest {

  @Test
  void lotQueryValidatesPageAndSize() {
    assertThatThrownBy(
            () ->
                new LotQuery(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    -1,
                    20,
                    LotSort.ENDING_SOON))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void lotPageCalculatesTotalPages() {
    LotPage page = new LotPage(List.of(), 0, 20, 45);
    assertThat(page.totalPages()).isEqualTo(3);
  }

  @Test
  void exceptionsExposeDetails() {
    InvalidTransitionException transition =
        new InvalidTransitionException(LotStatus.DRAFT, LotEvent.DELETE);
    assertThat(transition.from()).isEqualTo(LotStatus.DRAFT);
    assertThat(transition.event()).isEqualTo(LotEvent.DELETE);

    LotValidationException validation =
        new LotValidationException("bad", Map.of("field", "message"));
    assertThat(validation.details()).containsEntry("field", "message");

    assertThat(new ImageRequiredException()).hasMessageContaining("READY");
    assertThat(new ForbiddenLotAccessException("nope")).hasMessage("nope");
  }

  @Test
  void lotOptionalReserveAndForceStatus() {
    Lot lot =
        Lot.createDraft(
                1L,
                "Title",
                "Desc",
                1L,
                Money.fromCents(1000),
                Money.fromCents(100),
                Money.fromCents(1500),
                java.time.Instant.parse("2026-08-02T12:00:00Z"))
            .withId(1L)
            .withVersion(3L)
            .forceStatus(LotStatus.LIVE, java.time.Instant.parse("2026-08-02T12:00:00Z"));

    assertThat(lot.optionalReservePrice()).contains(Money.fromCents(1500));
    assertThat(lot.version()).isEqualTo(3L);
    assertThat(lot.status()).isEqualTo(LotStatus.LIVE);
  }
}
