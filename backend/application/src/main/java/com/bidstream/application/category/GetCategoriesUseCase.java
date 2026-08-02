package com.bidstream.application.category;

import com.bidstream.domain.category.Category;
import com.bidstream.domain.category.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCategoriesUseCase {

  private final CategoryRepository categoryRepository;

  public GetCategoriesUseCase(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  @Transactional(readOnly = true)
  public List<Category> execute() {
    return categoryRepository.findRootCategoriesWithChildren();
  }
}
