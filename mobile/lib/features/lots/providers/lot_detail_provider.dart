import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/cache/lot_cache_service.dart';
import '../data/lot_dto.dart';
import '../data/lot_image_dto.dart';
import 'lots_list_provider.dart';

class LotDetailState {
  const LotDetailState({
    required this.lot,
    this.offline = false,
    this.cachedAt,
    this.stalePrice = false,
  });

  final LotDto lot;
  final bool offline;
  final DateTime? cachedAt;
  final bool stalePrice;
}

final lotDetailProvider =
    AsyncNotifierProvider.family<LotDetailNotifier, LotDetailState, int>(
      LotDetailNotifier.new,
    );

class LotDetailNotifier extends FamilyAsyncNotifier<LotDetailState, int> {
  LotCacheService get _cache => ref.read(lotCacheServiceProvider);

  @override
  Future<LotDetailState> build(int lotId) => _load(lotId);

  Future<void> reload() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() => _load(arg));
  }

  Future<LotDetailState> _load(int lotId) async {
    final cached = await _cache.readLot(lotId);
    if (cached != null) {
      state = AsyncData(
        LotDetailState(
          lot: cached.data,
          offline: true,
          cachedAt: cached.cachedAt,
          stalePrice: cached.isStale,
        ),
      );
    }

    try {
      final lot = await ref.read(lotsApiProvider).fetchLot(lotId);
      await _cache.writeLot(lot);
      return LotDetailState(lot: lot);
    } on DioException {
      if (cached != null) {
        return LotDetailState(
          lot: cached.data,
          offline: true,
          cachedAt: cached.cachedAt,
          stalePrice: cached.isStale,
        );
      }
      rethrow;
    }
  }

  void applyOptimisticImages(List<LotImageDto> images) {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData(
      current.copyWith(lot: current.lot.copyWith(images: images)),
    );
  }

  void applyOptimisticOrder(List<int> orderIds) {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    final byId = {for (final image in current.lot.images) image.id: image};
    final reordered = orderIds
        .map((id) => byId[id])
        .whereType<LotImageDto>()
        .toList();
    applyOptimisticImages(reordered);
  }
}

extension on LotDetailState {
  LotDetailState copyWith({
    LotDto? lot,
    bool? offline,
    DateTime? cachedAt,
    bool? stalePrice,
  }) {
    return LotDetailState(
      lot: lot ?? this.lot,
      offline: offline ?? this.offline,
      cachedAt: cachedAt ?? this.cachedAt,
      stalePrice: stalePrice ?? this.stalePrice,
    );
  }
}
