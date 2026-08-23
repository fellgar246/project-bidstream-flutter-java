import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/categories/data/category_dto.dart';
import '../../features/lots/data/lot_dto.dart';
import '../../features/lots/data/lot_filters.dart';
import 'app_database.dart';

const cacheTtl = Duration(hours: 24);

final lotCacheServiceProvider = Provider<LotCacheService>((ref) {
  throw UnimplementedError('LotCacheService must be wired with database');
});

class CacheReadResult<T> {
  const CacheReadResult({
    required this.data,
    required this.fromCache,
    this.cachedAt,
    this.isStale = false,
    this.offline = false,
  });

  final T data;
  final bool fromCache;
  final DateTime? cachedAt;
  final bool isStale;
  final bool offline;
}

class LotCacheService {
  LotCacheService(this._db);

  final AppDatabase _db;

  bool isExpired(DateTime? cachedAt) {
    if (cachedAt == null) return true;
    return DateTime.now().difference(cachedAt) > cacheTtl;
  }

  Future<CacheReadResult<List<LotDto>>?> readLotsList() async {
    final cachedAt = await _db.lotsListCachedAt();
    if (cachedAt == null || isExpired(cachedAt)) {
      return null;
    }
    final jsonList = await _db.readAllLots();
    return CacheReadResult(
      data: jsonList.map(LotDto.fromJson).toList(),
      fromCache: true,
      cachedAt: cachedAt,
    );
  }

  Future<void> writeLotsList(List<LotDto> lots) async {
    final now = DateTime.now();
    for (final lot in lots) {
      await _db.upsertLot(lot.id, _lotToJson(lot), now);
    }
    await _db.touchLotsList(now);
  }

  Future<void> writeLot(LotDto lot) async {
    await _db.upsertLot(lot.id, _lotToJson(lot), DateTime.now());
  }

  Future<CacheReadResult<LotDto>?> readLot(int id) async {
    final cachedAt = await _db.lotCachedAt(id);
    if (cachedAt == null || isExpired(cachedAt)) {
      return null;
    }
    final json = await _db.readLot(id);
    if (json == null) return null;
    final lot = LotDto.fromJson(json);
    return CacheReadResult(
      data: lot,
      fromCache: true,
      cachedAt: cachedAt,
      isStale: lot.status == 'LIVE',
    );
  }

  Future<CacheReadResult<List<CategoryDto>>?> readCategories() async {
    final cachedAt = await _db.categoriesCachedAt();
    if (cachedAt == null || isExpired(cachedAt)) {
      return null;
    }
    final jsonList = await _db.readCategories();
    return CacheReadResult(
      data: jsonList.map(CategoryDto.fromJson).toList(),
      fromCache: true,
      cachedAt: cachedAt,
    );
  }

  Future<void> writeCategories(List<CategoryDto> categories) async {
    final now = DateTime.now();
    await _db.upsertCategories(
      categories.map((c) => {'id': c.id, 'name': c.name}).toList(),
      now,
    );
    await _db.touchCategories(now);
  }

  Future<void> clearAll() => _db.clearAll();

  Map<String, dynamic> _lotToJson(LotDto lot) {
    return {
      'id': lot.id,
      'title': lot.title,
      'description': lot.description,
      'category': {'id': lot.category.id, 'name': lot.category.name},
      'seller': {'id': lot.seller.id, 'displayName': lot.seller.displayName},
      'startingPrice': lot.startingPrice,
      'minIncrement': lot.minIncrement,
      'currentPrice': lot.currentPrice,
      'bidCount': lot.bidCount,
      'hasReserve': lot.hasReserve,
      'reserveMet': lot.reserveMet,
      'status': lot.status,
      'scheduledStartAt': lot.scheduledStartAt,
      'scheduledEndAt': lot.scheduledEndAt,
      'actualEndAt': lot.actualEndAt,
      'images': lot.images
          .map(
            (image) => {
              'id': image.id,
              'url': image.url,
              'thumbnailUrl': image.thumbnailUrl,
              'position': image.position,
            },
          )
          .toList(),
      'watched': lot.watched,
      'canEdit': lot.canEdit,
      'canBid': lot.canBid,
    };
  }
}

class CachedLotFilters {
  static bool matches(LotDto lot, LotFilters filters) {
    final query = filters.query?.trim().toLowerCase();
    if (query != null && query.isNotEmpty) {
      if (!lot.title.toLowerCase().contains(query) &&
          !lot.description.toLowerCase().contains(query)) {
        return false;
      }
    }
    if (filters.categoryId != null && lot.category.id != filters.categoryId) {
      return false;
    }
    return true;
  }
}
