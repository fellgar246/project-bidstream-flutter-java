package com.bidstream.domain.category;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CategoryTest {

  @Test
  void record_exposesChildrenDefensively() {
    Category child = new Category(2L, "child", "Child");
    Category root = new Category(1L, "root", "Root", List.of(child));

    assertThat(root.children()).containsExactly(child);
    assertThat(root.id()).isEqualTo(1L);
    assertThat(root.slug()).isEqualTo("root");
  }
}
