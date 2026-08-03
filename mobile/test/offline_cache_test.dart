import 'package:bidstream/core/cache/app_database.dart';
import 'package:bidstream/core/cache/lot_cache_service.dart';
import 'package:bidstream/features/lots/data/lot_dto.dart';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late AppDatabase db;
  late LotCacheService cache;

  setUp(() {
    db = AppDatabase(NativeDatabase.memory());
    cache = LotCacheService(db);
  });

  tearDown(() async {
    await db.close();
  });

  LotDto sampleLot({int id = 1, String status = 'SCHEDULED'}) {
    return LotDto(
      id: id,
      title: 'Test lot $id',
      description: 'Description',
      category: const LotCategoryDto(id: 1, name: 'Art'),
      seller: const LotSellerDto(id: 2, displayName: 'Seller'),
      startingPrice: '100.00',
      minIncrement: '5.00',
      currentPrice: '150.00',
      bidCount: 3,
      hasReserve: false,
      reserveMet: true,
      status: status,
      watched: false,
      canEdit: false,
      canBid: status == 'LIVE',
    );
  }

  test('returns cached lots within TTL', () async {
    await cache.writeLotsList([sampleLot()]);
    final result = await cache.readLotsList();
    expect(result, isNotNull);
    expect(result!.data, hasLength(1));
    expect(result.fromCache, isTrue);
  });

  test('expired cache is not returned', () async {
    final lot = sampleLot();
    final expiredAt = DateTime.now().subtract(const Duration(hours: 25));
    await db.upsertLot(lot.id, {
      'id': lot.id,
      'title': lot.title,
      'description': lot.description,
      'category': {'id': 1, 'name': 'Art'},
      'seller': {'id': 2, 'displayName': 'Seller'},
      'startingPrice': lot.startingPrice,
      'minIncrement': lot.minIncrement,
      'currentPrice': lot.currentPrice,
      'bidCount': lot.bidCount,
      'hasReserve': false,
      'reserveMet': true,
      'status': lot.status,
      'watched': false,
      'canEdit': false,
      'canBid': false,
      'images': [],
    }, expiredAt);
    await db.touchLotsList(expiredAt);

    final result = await cache.readLotsList();
    expect(result, isNull);
  });

  test('LIVE lot from cache is marked stale', () async {
    await cache.writeLot(sampleLot(status: 'LIVE'));
    final result = await cache.readLot(1);
    expect(result, isNotNull);
    expect(result!.isStale, isTrue);
  });

  test('clearAll removes cached data', () async {
    await cache.writeLotsList([sampleLot()]);
    await cache.clearAll();
    expect(await cache.readLotsList(), isNull);
  });
}
