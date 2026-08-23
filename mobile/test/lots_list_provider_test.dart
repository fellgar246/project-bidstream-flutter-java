import 'package:bidstream/core/cache/app_database.dart';
import 'package:bidstream/core/cache/lot_cache_service.dart';
import 'package:bidstream/core/network/dio_client.dart';
import 'package:drift/native.dart';
import 'package:bidstream/features/lots/data/lot_dto.dart';
import 'package:bidstream/features/lots/data/lot_filters.dart';
import 'package:bidstream/features/lots/data/lots_api.dart';
import 'package:bidstream/features/lots/providers/lots_list_provider.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test(
    'ca0311 rapid filter change keeps only the latest filter results',
    () async {
      final db = AppDatabase(NativeDatabase.memory());
      addTearDown(db.close);
      final container = ProviderContainer(
        overrides: [
          lotsApiProvider.overrideWith((ref) => _DelayedLotsApi()),
          lotCacheServiceProvider.overrideWithValue(LotCacheService(db)),
        ],
      );
      addTearDown(container.dispose);

      const filtersA = LotFilters(status: 'LIVE');
      const filtersB = LotFilters(status: 'SCHEDULED');

      final subscription = container.listen(
        lotsListProvider(filtersA),
        (_, _) {},
        fireImmediately: true,
      );
      await Future<void>.delayed(Duration.zero);

      final stateB = await container.read(lotsListProvider(filtersB).future);

      expect(stateB.items, hasLength(1));
      expect(stateB.items.single.status, 'SCHEDULED');
      expect(stateB.items.single.id, 2);

      subscription.close();
      final stateA = container.read(lotsListProvider(filtersA));
      expect(stateA.valueOrNull?.items.single.status, 'LIVE');
    },
  );
}

class _DelayedLotsApi extends LotsApi {
  _DelayedLotsApi() : super(DioClient(baseUrl: 'http://test'));

  @override
  Future<LotPageDto> fetchLots(
    LotFilters filters,
    int page, {
    bool facets = false,
  }) async {
    await Future<void>.delayed(const Duration(milliseconds: 30));
    return LotPageDto(
      content: [
        LotDto(
          id: filters.status == 'LIVE' ? 1 : 2,
          title: filters.status,
          description: 'Desc',
          category: const LotCategoryDto(id: 1, name: 'Art'),
          seller: const LotSellerDto(id: 1, displayName: 'Seller'),
          startingPrice: '10.00',
          minIncrement: '1.00',
          currentPrice: '10.00',
          bidCount: 0,
          hasReserve: false,
          reserveMet: true,
          status: filters.status,
          watched: false,
          canEdit: false,
          canBid: false,
        ),
      ],
      page: const LotPageMetadata(
        number: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      ),
    );
  }
}
