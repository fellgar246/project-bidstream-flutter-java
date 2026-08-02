package com.bidstream.domain.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void fromString_parsesDecimalToCents() {
    assertThat(Money.fromString("1250.00").cents()).isEqualTo(125000L);
  }

  @Test
  void fromCents_formatsDecimalString() {
    assertThat(Money.fromCents(125000).toString()).isEqualTo("1250.00");
  }

  @Test
  void add_avoidsFloatingPointDrift() {
    Money sum = Money.fromString("0.1").add(Money.fromString("0.2"));
    assertThat(sum.toString()).isEqualTo("0.30");
  }

  @Test
  void compareTo_ordersByCents() {
    Money lower = Money.fromString("10.00");
    Money higher = Money.fromString("20.00");
    assertThat(lower.compareTo(higher)).isNegative();
    assertThat(higher.compareTo(lower)).isPositive();
  }

  @Test
  void subtract_reducesAmount() {
    Money result = Money.fromString("5.00").subtract(Money.fromString("1.25"));
    assertThat(result.toString()).isEqualTo("3.75");
  }

  @Test
  void fromString_rejectsBlankInput() {
    assertThatThrownBy(() -> Money.fromString(" ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void equalsAndHashCode_useCents() {
    Money first = Money.fromCents(100);
    Money second = Money.fromCents(100);
    Money third = Money.fromCents(200);

    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
    assertThat(first).isNotEqualTo(third);
  }

  @Test
  void fromString_rejectsInvalidScale() {
    assertThatThrownBy(() -> Money.fromString("1.234")).isInstanceOf(ArithmeticException.class);
  }
}
