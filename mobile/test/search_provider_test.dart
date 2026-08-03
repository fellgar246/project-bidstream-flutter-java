import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:bidstream/features/lots/data/lot_dto.dart';
import 'package:bidstream/features/lots/data/lot_filters.dart';
import 'package:bidstream/features/lots/data/lots_api.dart';
import 'package:bidstream/features/lots/providers/lots_list_provider.dart';
import 'package:bidstream/features/lots/providers/search_provider.dart';

void main() {
  test('debounce keeps only the latest search response', () async {
    final container = ProviderContainer(
      overrides: [
        lotsApiProvider.overrideWith((ref) => _FakeLotsApi()),
      ],
    );
    addTearDown(container.dispose);

    final notifier = container.read(searchProvider(const LotFilters()).notifier);
    notifier.updateQuery('reloj');
    notifier.updateQuery('reloj suizo');

    await Future<void>.delayed(const Duration(milliseconds: 400));

    final state = container.read(searchProvider(const LotFilters()));
    expect(state.query, 'reloj suizo');
    expect(state.items.single.title, 'Reloj suizo antiguo');
  });
}

class _FakeLotsApi implements LotsApi {
  @override
  Future<LotPageDto> fetchLots(LotFilters filters, int page, {bool facets = false}) async {
    final query = filters.query ?? '';
    if (query.contains('suizo')) {
      return const LotPageDto(
        content: [
          LotDto(
            id: 1,
            title: 'Reloj suizo antiguo',
            description: 'Desc',
            category: LotCategoryDto(id: 1, name: 'Relojes'),
            seller: LotSellerDto(id: 1, displayName: 'Seller'),
            startingPrice: '100.00',
            minIncrement: '5.00',
            currentPrice: '100.00',
            bidCount: 0,
            hasReserve: false,
            reserveMet: false,
            status: 'LIVE',
            watched: false,
            canEdit: false,
            canBid: false,
          ),
        ],
        page: LotPageMetadata(number: 0, size: 20, totalElements: 1, totalPages: 1),
      );
    }
    return const LotPageDto(
      content: [],
      page: LotPageMetadata(number: 0, size: 20, totalElements: 0, totalPages: 0),
    );
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}
