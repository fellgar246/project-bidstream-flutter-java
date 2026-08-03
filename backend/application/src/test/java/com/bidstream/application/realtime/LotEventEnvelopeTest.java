package com.bidstream.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LotEventEnvelopeTest {

  @Test
  void eventIds_deriveFromBidId() {
    assertThat(LotEventEnvelope.eventIdForBid(5L)).isEqualTo(10L);
    assertThat(LotEventEnvelope.eventIdForExtension(5L)).isEqualTo(11L);
  }
}
