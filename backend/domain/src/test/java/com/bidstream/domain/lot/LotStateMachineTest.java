package com.bidstream.domain.lot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class LotStateMachineTest {

  @ParameterizedTest
  @MethodSource("allowedTransitions")
  void allowedTransition_succeeds(LotStatus from, LotEvent event, LotStatus expected) {
    assertThat(LotStateMachine.isAllowed(from, event)).isTrue();
    assertThat(LotStateMachine.transition(from, event)).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("disallowedTransitions")
  void disallowedTransition_throwsInvalidTransition(LotStatus from, LotEvent event) {
    assertThat(LotStateMachine.isAllowed(from, event)).isFalse();
    assertThatThrownBy(() -> LotStateMachine.transition(from, event))
        .isInstanceOf(InvalidTransitionException.class)
        .satisfies(
            ex -> {
              InvalidTransitionException ite = (InvalidTransitionException) ex;
              assertThat(ite.from()).isEqualTo(from);
              assertThat(ite.event()).isEqualTo(event);
            });
  }

  @ParameterizedTest
  @EnumSource(LotStatus.class)
  void updateAndDelete_neverAllowed(LotStatus status) {
    assertThat(LotStateMachine.isAllowed(status, LotEvent.UPDATE)).isFalse();
    assertThat(LotStateMachine.isAllowed(status, LotEvent.DELETE)).isFalse();
  }

  private static Stream<Arguments> allowedTransitions() {
    return Stream.of(
        Arguments.of(LotStatus.DRAFT, LotEvent.SCHEDULE, LotStatus.SCHEDULED),
        Arguments.of(LotStatus.DRAFT, LotEvent.CANCEL, LotStatus.CANCELLED),
        Arguments.of(LotStatus.SCHEDULED, LotEvent.CANCEL, LotStatus.CANCELLED),
        Arguments.of(LotStatus.SCHEDULED, LotEvent.START, LotStatus.LIVE),
        Arguments.of(LotStatus.LIVE, LotEvent.CLOSE_SOLD, LotStatus.CLOSED_SOLD),
        Arguments.of(LotStatus.LIVE, LotEvent.CLOSE_NO_SALE, LotStatus.CLOSED_NO_SALE));
  }

  private static Stream<Arguments> disallowedTransitions() {
    Stream.Builder<Arguments> builder = Stream.builder();
    for (LotStatus from : LotStatus.values()) {
      for (LotEvent event : LotEvent.values()) {
        if (!LotStateMachine.isAllowed(from, event)) {
          builder.add(Arguments.of(from, event));
        }
      }
    }
    return builder.build();
  }
}
