import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:bidstream/core/l10n/app_strings.dart';
import 'package:bidstream/core/network/api_exception.dart';
import 'package:bidstream/features/categories/data/category_dto.dart';
import 'package:bidstream/features/categories/presentation/categories_screen.dart';
import 'package:bidstream/features/categories/providers/categories_provider.dart';

void main() {
  testWidgets('categories screen shows loading state', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          categoriesProvider.overrideWith(_LoadingCategoriesNotifier.new),
        ],
        child: const MaterialApp(home: CategoriesScreen()),
      ),
    );

    expect(find.text(AppStrings.categoriesLoading), findsOneWidget);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });

  testWidgets('categories screen shows error state', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          categoriesProvider.overrideWith(_ErrorCategoriesNotifier.new),
        ],
        child: const MaterialApp(home: CategoriesScreen()),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text(AppStrings.categoriesError), findsOneWidget);
    expect(find.text(AppStrings.retry), findsOneWidget);
  });

  testWidgets('categories screen shows data state', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          categoriesProvider.overrideWith(_DataCategoriesNotifier.new),
        ],
        child: const MaterialApp(home: CategoriesScreen()),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Art'), findsOneWidget);
    await tester.tap(find.text('Art'));
    await tester.pumpAndSettle();
    expect(find.text('Paintings'), findsOneWidget);
  });
}

class _LoadingCategoriesNotifier extends CategoriesNotifier {
  @override
  Future<List<CategoryDto>> build() {
    return Completer<List<CategoryDto>>().future;
  }
}

class _ErrorCategoriesNotifier extends CategoriesNotifier {
  @override
  Future<List<CategoryDto>> build() async {
    throw ApiException(
      code: 'network_error',
      message: 'Backend unavailable',
      details: const {},
    );
  }
}

class _DataCategoriesNotifier extends CategoriesNotifier {
  @override
  Future<List<CategoryDto>> build() async {
    return const [
      CategoryDto(
        id: 1,
        slug: 'art',
        name: 'Art',
        children: [
          CategoryDto(id: 2, slug: 'art-paintings', name: 'Paintings', children: []),
        ],
      ),
    ];
  }
}
