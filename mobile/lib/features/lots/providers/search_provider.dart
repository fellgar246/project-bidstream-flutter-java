import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/lot_dto.dart';
import '../data/lot_filters.dart';
import '../providers/lots_list_provider.dart';

class SearchState {
  const SearchState({
    required this.items,
    required this.isLoading,
    this.error,
    this.facets,
    this.query = '',
  });

  final List<LotDto> items;
  final bool isLoading;
  final Object? error;
  final LotFacetsDto? facets;
  final String query;

  SearchState copyWith({
    List<LotDto>? items,
    bool? isLoading,
    Object? error,
    LotFacetsDto? facets,
    String? query,
  }) {
    return SearchState(
      items: items ?? this.items,
      isLoading: isLoading ?? this.isLoading,
      error: error,
      facets: facets ?? this.facets,
      query: query ?? this.query,
    );
  }
}

final searchProvider =
    NotifierProvider.family<SearchNotifier, SearchState, LotFilters>(
      SearchNotifier.new,
    );

class SearchNotifier extends FamilyNotifier<SearchState, LotFilters> {
  Timer? _debounce;
  int _generation = 0;

  @override
  SearchState build(LotFilters filters) {
    ref.onDispose(() => _debounce?.cancel());
    return SearchState(
      items: const [],
      isLoading: false,
      query: filters.query ?? '',
    );
  }

  void updateQuery(String query) {
    state = state.copyWith(query: query, isLoading: true, error: null);
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 350), () {
      _runSearch(query);
    });
  }

  Future<void> _runSearch(String query) async {
    final generation = ++_generation;
    final trimmed = query.trim();
    final filters = trimmed.isEmpty
        ? arg.copyWith(query: '')
        : arg.copyWith(query: trimmed);

    try {
      final page = await ref
          .read(lotsApiProvider)
          .fetchLots(filters, 0, facets: true);
      if (generation != _generation) {
        return;
      }
      state = SearchState(
        items: page.content,
        isLoading: false,
        facets: page.facets,
        query: trimmed,
      );
    } catch (error) {
      if (generation != _generation) {
        return;
      }
      state = state.copyWith(isLoading: false, error: error);
    }
  }

  void clearFilters() {
    updateQuery('');
  }
}
