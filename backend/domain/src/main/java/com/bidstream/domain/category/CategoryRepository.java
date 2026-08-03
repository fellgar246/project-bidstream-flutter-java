package com.bidstream.domain.category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

  List<Category> findRootCategoriesWithChildren();

  Optional<Category> findById(long id);
}
