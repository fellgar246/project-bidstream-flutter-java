import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/lot_dto.dart';
import '../data/lot_filters.dart';
import '../data/lots_api.dart';
import '../../../core/network/dio_provider.dart';

final lotsApiProvider = Provider<LotsApi>((ref) {
  return LotsApi(ref.watch(dioClientProvider));
});

class LotsPageState {
  const LotsPageState({
    required this.items,
    required this.page,
    required this.hasMore,
    required this.isLoadingMore,
  });

  final List<LotDto> items;
  final int page;
  final bool hasMore;
  final bool isLoadingMore;

  LotsPageState copyWith({
    List<LotDto>? items,
    int? page,
    bool? hasMore,
    bool? isLoadingMore,
  }) {
    return LotsPageState(
      items: items ?? this.items,
      page: page ?? this.page,
      hasMore: hasMore ?? this.hasMore,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
    );
  }
}

final lotsListProvider = AsyncNotifierProvider.family<LotsListNotifier, LotsPageState, LotFilters>(
  LotsListNotifier.new,
);

class LotsListNotifier extends FamilyAsyncNotifier<LotsPageState, LotFilters> {
  int _requestGeneration = 0;

  @override
  Future<LotsPageState> build(LotFilters filters) => _loadFirstPage(filters);

  Future<void> reload() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() => _loadFirstPage(arg));
  }

  Future<void> loadMore() async {
    final current = state.valueOrNull;
    if (current == null || !current.hasMore || current.isLoadingMore) {
      return;
    }

    state = AsyncData(current.copyWith(isLoadingMore: true));
    final generation = _requestGeneration;

    try {
      final page = await ref.read(lotsApiProvider).fetchLots(arg, current.page + 1);
      if (generation != _requestGeneration) {
        return;
      }
      final updatedItems = [...current.items, ...page.content];
      state = AsyncData(
        LotsPageState(
          items: updatedItems,
          page: page.page.number,
          hasMore: page.page.number + 1 < page.page.totalPages,
          isLoadingMore: false,
        ),
      );
    } catch (error, stackTrace) {
      if (generation != _requestGeneration) {
        return;
      }
      state = AsyncError(error, stackTrace);
    }
  }

  Future<LotsPageState> _loadFirstPage(LotFilters filters) async {
    final generation = ++_requestGeneration;
    final page = await ref.read(lotsApiProvider).fetchLots(filters, 0);
    if (generation != _requestGeneration) {
      throw StateError('Stale lots response discarded');
    }
    return LotsPageState(
      items: page.content,
      page: page.page.number,
      hasMore: page.page.number + 1 < page.page.totalPages,
      isLoadingMore: false,
    );
  }
}
