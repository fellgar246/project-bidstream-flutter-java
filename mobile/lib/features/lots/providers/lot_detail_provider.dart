import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/lot_dto.dart';
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
}
