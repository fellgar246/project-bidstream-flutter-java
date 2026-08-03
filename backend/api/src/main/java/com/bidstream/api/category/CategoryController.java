package com.bidstream.api.category;

import com.bidstream.application.cache.CategoryCachePort;
import com.bidstream.application.category.GetCategoriesUseCase;
import com.bidstream.domain.category.Category;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

  private static final Duration CACHE_TTL = Duration.ofHours(1);

  private final GetCategoriesUseCase getCategoriesUseCase;
  private final CategoryCachePort categoryCachePort;
  private final ObjectMapper objectMapper;

  public CategoryController(
      GetCategoriesUseCase getCategoriesUseCase,
      CategoryCachePort categoryCachePort,
      ObjectMapper objectMapper) {
    this.getCategoriesUseCase = getCategoriesUseCase;
    this.categoryCachePort = categoryCachePort;
    this.objectMapper = objectMapper;
  }

  @GetMapping
  public List<CategoryResponse> listCategories() throws JsonProcessingException {
    var cached = categoryCachePort.getTreeJson();
    if (cached.isPresent()) {
      return objectMapper.readValue(
          cached.get(),
          objectMapper
              .getTypeFactory()
              .constructCollectionType(List.class, CategoryResponse.class));
    }
    List<CategoryResponse> tree =
        getCategoriesUseCase.execute().stream().map(this::toResponse).toList();
    categoryCachePort.putTreeJson(objectMapper.writeValueAsString(tree), CACHE_TTL);
    return tree;
  }

  private CategoryResponse toResponse(Category category) {
    List<CategoryResponse> children = category.children().stream().map(this::toResponse).toList();
    return new CategoryResponse(category.id(), category.slug(), category.name(), children);
  }
}
