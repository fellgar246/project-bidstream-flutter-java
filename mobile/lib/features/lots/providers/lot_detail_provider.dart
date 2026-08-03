import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/lot_dto.dart';
import '../data/lot_image_dto.dart';
import 'lots_list_provider.dart';

final lotDetailProvider = AsyncNotifierProvider.family<LotDetailNotifier, LotDto, int>(
  LotDetailNotifier.new,
);

class LotDetailNotifier extends FamilyAsyncNotifier<LotDto, int> {
  @override
  Future<LotDto> build(int lotId) {
    return ref.read(lotsApiProvider).fetchLot(lotId);
  }

  Future<void> reload() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() => ref.read(lotsApiProvider).fetchLot(arg));
  }

  void applyOptimisticImages(List<LotImageDto> images) {
    final current = state.value;
    if (current == null) {
      return;
    }
    state = AsyncData(current.copyWith(images: images));
  }

  void applyOptimisticOrder(List<int> orderIds) {
    final current = state.value;
    if (current == null) {
      return;
    }
    final byId = {for (final image in current.images) image.id: image};
    final reordered = orderIds.map((id) => byId[id]).whereType<LotImageDto>().toList();
    applyOptimisticImages(reordered);
  }
}
