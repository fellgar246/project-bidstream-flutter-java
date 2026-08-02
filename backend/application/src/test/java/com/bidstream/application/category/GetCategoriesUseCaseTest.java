package com.bidstream.application.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bidstream.domain.category.Category;
import com.bidstream.domain.category.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCategoriesUseCaseTest {

  @Mock private CategoryRepository categoryRepository;

  @InjectMocks private GetCategoriesUseCase getCategoriesUseCase;

  @Test
  void execute_returnsCategoriesFromRepository() {
    Category child = new Category(2L, "art-paintings", "Paintings");
    Category root = new Category(1L, "art", "Art", List.of(child));
    when(categoryRepository.findRootCategoriesWithChildren()).thenReturn(List.of(root));

    List<Category> result = getCategoriesUseCase.execute();

    assertThat(result).containsExactly(root);
  }
}
