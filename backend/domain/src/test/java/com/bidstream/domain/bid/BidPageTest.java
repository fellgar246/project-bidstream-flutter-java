package com.bidstream.domain.bid;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BidPageTest {

  @Test
  void totalPages_computesFromSizeAndTotal() {
    BidPage page = new BidPage(List.of(), 0, 20, 45);
    assertThat(page.totalPages()).isEqualTo(3);
  }

  @Test
  void totalPages_zeroWhenSizeInvalid() {
    BidPage page = new BidPage(List.of(), 0, 0, 10);
    assertThat(page.totalPages()).isZero();
  }
}
