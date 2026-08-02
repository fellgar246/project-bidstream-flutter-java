package com.bidstream.infrastructure.persistence.category;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {

  @Query(
      """
      SELECT c FROM CategoryEntity c
      LEFT JOIN FETCH c.children ch
      WHERE c.parent IS NULL
      ORDER BY c.name ASC, c.id ASC
      """)
  List<CategoryEntity> findRootsWithChildren();

  boolean existsBySlug(String slug);

  java.util.Optional<CategoryEntity> findBySlug(String slug);
}
