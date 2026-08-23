import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_provider.dart';
import '../data/categories_api.dart';
import '../data/category_dto.dart';

final categoriesApiProvider = Provider<CategoriesApi>((ref) {
  return CategoriesApi(ref.watch(dioClientProvider));
});

final categoriesProvider =
    AsyncNotifierProvider<CategoriesNotifier, List<CategoryDto>>(
      CategoriesNotifier.new,
    );

class CategoriesNotifier extends AsyncNotifier<List<CategoryDto>> {
  @override
  Future<List<CategoryDto>> build() => _load();

  Future<void> reload() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(_load);
  }

  Future<List<CategoryDto>> _load() {
    return ref.read(categoriesApiProvider).fetchCategories();
  }
}
