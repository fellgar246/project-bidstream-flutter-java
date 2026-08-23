import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/cache/lot_cache_service.dart';
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
    this.offline = false,
    this.cachedAt,
  });

  final List<LotDto> items;
  final int page;
  final bool hasMore;
  final bool isLoadingMore;
  final bool offline;
  final DateTime? cachedAt;

  LotsPageState copyWith({
    List<LotDto>? items,
    int? page,
    bool? hasMore,
    bool? isLoadingMore,
    bool? offline,
    DateTime? cachedAt,
  }) {
    return LotsPageState(
      items: items ?? this.items,
      page: page ?? this.page,
      hasMore: hasMore ?? this.hasMore,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
      offline: offline ?? this.offline,
      cachedAt: cachedAt ?? this.cachedAt,
    );
  }
}

final lotsListProvider =
    AsyncNotifierProvider.family<LotsListNotifier, LotsPageState, LotFilters>(
      LotsListNotifier.new,
    );

class LotsListNotifier extends FamilyAsyncNotifier<LotsPageState, LotFilters> {
  int _requestGeneration = 0;

  LotCacheService get _cache => ref.read(lotCacheServiceProvider);

  @override
  Future<LotsPageState> build(LotFilters filters) => _loadFirstPage(filters);

  Future<void> reload() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() => _loadFirstPage(arg));
  }

  Future<void> loadMore() async {
    final current = state.valueOrNull;
    if (current == null ||
        !current.hasMore ||
        current.isLoadingMore ||
        current.offline) {
      return;
    }

    state = AsyncData(current.copyWith(isLoadingMore: true));
    final generation = _requestGeneration;

    try {
      final page = await ref
          .read(lotsApiProvider)
          .fetchLots(arg, current.page + 1);
      if (generation != _requestGeneration) {
        return;
      }
      final updatedItems = [...current.items, ...page.content];
      await _cache.writeLotsList(updatedItems);
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
    final cached = await _cache.readLotsList();

    if (cached != null) {
      final filtered = cached.data
          .where((lot) => CachedLotFilters.matches(lot, filters))
          .toList();
      final initial = LotsPageState(
        items: filtered,
        page: 0,
        hasMore: false,
        isLoadingMore: false,
        offline: true,
        cachedAt: cached.cachedAt,
      );
      state = AsyncData(initial);
    }

    try {
      final page = await ref.read(lotsApiProvider).fetchLots(filters, 0);
      if (generation != _requestGeneration) {
        throw StateError('Stale lots response discarded');
      }
      await _cache.writeLotsList(page.content);
      return LotsPageState(
        items: page.content,
        page: page.page.number,
        hasMore: page.page.number + 1 < page.page.totalPages,
        isLoadingMore: false,
      );
    } on DioException {
      if (cached != null) {
        final filtered = cached.data
            .where((lot) => CachedLotFilters.matches(lot, filters))
            .toList();
        return LotsPageState(
          items: filtered,
          page: 0,
          hasMore: false,
          isLoadingMore: false,
          offline: true,
          cachedAt: cached.cachedAt,
        );
      }
      rethrow;
    }
  }
}
