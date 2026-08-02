package com.bidstream.domain.category;

import java.util.List;

public interface CategoryRepository {

  List<Category> findRootCategoriesWithChildren();
}
