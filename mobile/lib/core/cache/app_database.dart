import 'dart:convert';

import 'package:drift/drift.dart';

part 'app_database.g.dart';

class CachedLots extends Table {
  IntColumn get id => integer()();
  TextColumn get payload => text()();
  DateTimeColumn get cachedAt => dateTime()();

  @override
  Set<Column<Object>> get primaryKey => {id};
}

class CachedCategories extends Table {
  IntColumn get id => integer()();
  TextColumn get payload => text()();
  DateTimeColumn get cachedAt => dateTime()();

  @override
  Set<Column<Object>> get primaryKey => {id};
}

class CachedImages extends Table {
  IntColumn get lotId => integer()();
  IntColumn get imageId => integer()();
  TextColumn get url => text().nullable()();
  TextColumn get thumbnailUrl => text().nullable()();
  IntColumn get position => integer()();
  DateTimeColumn get cachedAt => dateTime()();

  @override
  Set<Column<Object>> get primaryKey => {lotId, imageId};
}

class SyncMeta extends Table {
  TextColumn get key => text()();
  DateTimeColumn get updatedAt => dateTime()();

  @override
  Set<Column<Object>> get primaryKey => {key};
}

@DriftDatabase(tables: [CachedLots, CachedCategories, CachedImages, SyncMeta])
class AppDatabase extends _$AppDatabase {
  AppDatabase(super.e);

  @override
  int get schemaVersion => 1;

  Future<void> upsertLot(int id, Map<String, dynamic> json, DateTime cachedAt) async {
    await into(cachedLots).insertOnConflictUpdate(
      CachedLotsCompanion(
        id: Value(id),
        payload: Value(jsonEncode(json)),
        cachedAt: Value(cachedAt),
      ),
    );
  }

  Future<Map<String, dynamic>?> readLot(int id) async {
    final row = await (select(cachedLots)..where((t) => t.id.equals(id))).getSingleOrNull();
    if (row == null) return null;
    return jsonDecode(row.payload) as Map<String, dynamic>;
  }

  Future<DateTime?> lotCachedAt(int id) async {
    final row = await (select(cachedLots)..where((t) => t.id.equals(id))).getSingleOrNull();
    return row?.cachedAt;
  }

  Future<List<Map<String, dynamic>>> readAllLots() async {
    final rows = await select(cachedLots).get();
    return rows.map((row) => jsonDecode(row.payload) as Map<String, dynamic>).toList();
  }

  Future<DateTime?> lotsListCachedAt() async {
    final row = await (select(syncMeta)..where((t) => t.key.equals('lots_list'))).getSingleOrNull();
    return row?.updatedAt;
  }

  Future<void> touchLotsList(DateTime updatedAt) async {
    await into(syncMeta).insertOnConflictUpdate(
      SyncMetaCompanion.insert(key: 'lots_list', updatedAt: updatedAt),
    );
  }

  Future<void> upsertCategories(List<Map<String, dynamic>> categories, DateTime cachedAt) async {
    await batch((batch) {
      batch.deleteWhere(cachedCategories, (_) => const Constant(true));
      for (final category in categories) {
        batch.insert(
          cachedCategories,
          CachedCategoriesCompanion(
            id: Value(category['id'] as int),
            payload: Value(jsonEncode(category)),
            cachedAt: Value(cachedAt),
          ),
        );
      }
    });
  }

  Future<List<Map<String, dynamic>>> readCategories() async {
    final rows = await select(cachedCategories).get();
    return rows.map((row) => jsonDecode(row.payload) as Map<String, dynamic>).toList();
  }

  Future<DateTime?> categoriesCachedAt() async {
    final row = await (select(syncMeta)..where((t) => t.key.equals('categories'))).getSingleOrNull();
    return row?.updatedAt;
  }

  Future<void> touchCategories(DateTime updatedAt) async {
    await into(syncMeta).insertOnConflictUpdate(
      SyncMetaCompanion.insert(key: 'categories', updatedAt: updatedAt),
    );
  }

  Future<void> clearAll() async {
    await delete(cachedLots).go();
    await delete(cachedCategories).go();
    await delete(cachedImages).go();
    await delete(syncMeta).go();
  }
}
