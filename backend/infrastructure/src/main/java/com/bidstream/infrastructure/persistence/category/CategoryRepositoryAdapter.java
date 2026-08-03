package com.bidstream.infrastructure.persistence.category;

import com.bidstream.domain.category.Category;
import com.bidstream.domain.category.CategoryRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryRepositoryAdapter implements CategoryRepository {

  private final CategoryJpaRepository jpaRepository;

  public CategoryRepositoryAdapter(CategoryJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public List<Category> findRootCategoriesWithChildren() {
    return jpaRepository.findRootsWithChildren().stream().map(this::toDomain).toList();
  }

  @Override
  public Optional<Category> findById(long id) {
    return jpaRepository
        .findById(id)
        .map(entity -> new Category(entity.getId(), entity.getSlug(), entity.getName()));
  }

  private Category toDomain(CategoryEntity entity) {
    List<Category> children =
        entity.getChildren().stream()
            .sorted(
                Comparator.comparing(CategoryEntity::getName).thenComparing(CategoryEntity::getId))
            .map(child -> new Category(child.getId(), child.getSlug(), child.getName()))
            .toList();
    return new Category(entity.getId(), entity.getSlug(), entity.getName(), children);
  }
}
