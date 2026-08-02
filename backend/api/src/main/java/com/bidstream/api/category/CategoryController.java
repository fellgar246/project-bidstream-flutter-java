package com.bidstream.api.category;

import com.bidstream.application.category.GetCategoriesUseCase;
import com.bidstream.domain.category.Category;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

  private final GetCategoriesUseCase getCategoriesUseCase;

  public CategoryController(GetCategoriesUseCase getCategoriesUseCase) {
    this.getCategoriesUseCase = getCategoriesUseCase;
  }

  @GetMapping
  public List<CategoryResponse> listCategories() {
    return getCategoriesUseCase.execute().stream().map(this::toResponse).toList();
  }

  private CategoryResponse toResponse(Category category) {
    List<CategoryResponse> children = category.children().stream().map(this::toResponse).toList();
    return new CategoryResponse(category.id(), category.slug(), category.name(), children);
  }
}
