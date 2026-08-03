package com.bidstream.infrastructure.seed;

import com.bidstream.infrastructure.persistence.category.CategoryEntity;
import com.bidstream.infrastructure.persistence.category.CategoryJpaRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CategorySeeder {

  private final CategoryJpaRepository categoryRepository;

  public CategorySeeder(CategoryJpaRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  @Transactional
  public void seed() {
    Instant now = Instant.now();

    Map<String, String[]> catalog = new LinkedHashMap<>();
    catalog.put("art", new String[] {"Paintings", "Sculptures", "Prints"});
    catalog.put("collectibles", new String[] {"Coins", "Stamps", "Memorabilia"});
    catalog.put("electronics", new String[] {"Computers", "Phones", "Audio"});
    catalog.put("jewelry", new String[] {"Rings", "Necklaces", "Watches"});
    catalog.put("furniture", new String[] {"Chairs", "Tables", "Lamps"});
    catalog.put("books", new String[] {"First Editions", "Comics", "Manuscripts"});
    catalog.put("sports", new String[] {"Memorabilia", "Equipment", "Cards"});
    catalog.put("automotive", new String[] {"Classic Cars", "Parts", "Models"});

    for (Map.Entry<String, String[]> rootEntry : catalog.entrySet()) {
      String rootSlug = rootEntry.getKey();
      CategoryEntity root = ensureCategory(rootSlug, capitalize(rootSlug), null, now);

      for (String childName : rootEntry.getValue()) {
        String childSlug = rootSlug + "-" + slugify(childName);
        ensureCategory(childSlug, childName, root, now);
      }
    }
  }

  private CategoryEntity ensureCategory(
      String slug, String name, CategoryEntity parent, Instant now) {
    return categoryRepository
        .findBySlug(slug)
        .orElseGet(
            () -> {
              CategoryEntity entity = new CategoryEntity();
              entity.setSlug(slug);
              entity.setName(name);
              entity.setParent(parent);
              entity.setCreatedAt(now);
              entity.setUpdatedAt(now);
              return categoryRepository.save(entity);
            });
  }

  private static String capitalize(String value) {
    if (value.isEmpty()) {
      return value;
    }
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }

  private static String slugify(String value) {
    return value.toLowerCase().replace(' ', '-');
  }
}
