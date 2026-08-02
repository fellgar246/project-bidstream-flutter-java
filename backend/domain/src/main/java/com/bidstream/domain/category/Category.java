package com.bidstream.domain.category;

import java.util.List;

public record Category(long id, String slug, String name, List<Category> children) {

  public Category {
    children = children == null ? List.of() : List.copyOf(children);
  }

  public Category(long id, String slug, String name) {
    this(id, slug, name, List.of());
  }
}
