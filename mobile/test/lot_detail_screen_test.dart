import 'package:bidstream/features/lots/data/lot_dto.dart';
import 'package:bidstream/features/lots/presentation/lot_detail_screen.dart';
import 'package:bidstream/features/lots/providers/lot_detail_provider.dart';
import 'package:bidstream/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'l10n_test_helper.dart';

void main() {
  final en = lookupAppLocalizations(const Locale('en'));

  testWidgets('ca0312 edit button only appears when canEdit is true', (tester) async {
    await pumpLocalized(
      tester,
      ProviderScope(
        overrides: [
          lotDetailProvider.overrideWith(_EditableLotNotifier.new),
        ],
        child: const LotDetailScreen(lotId: 42),
      ),
      locale: const Locale('en'),
    );
    expect(find.text(en.lotEdit), findsOneWidget);
  });

  testWidgets('ca0312 edit button hidden when canEdit is false', (tester) async {
    await pumpLocalized(
      tester,
      ProviderScope(
        overrides: [
          lotDetailProvider.overrideWith(_ReadOnlyLotNotifier.new),
        ],
        child: const LotDetailScreen(lotId: 42),
      ),
      locale: const Locale('en'),
    );
    expect(find.text(en.lotEdit), findsNothing);
  });
}

class _EditableLotNotifier extends LotDetailNotifier {
  @override
  Future<LotDto> build(int lotId) async => _sampleLot(canEdit: true);
}

class _ReadOnlyLotNotifier extends LotDetailNotifier {
  @override
  Future<LotDto> build(int lotId) async => _sampleLot(canEdit: false);
}

LotDto _sampleLot({required bool canEdit}) {
  return LotDto(
    id: 42,
    title: 'Vintage watch',
    description: 'Nice item',
    category: const LotCategoryDto(id: 1, name: 'Watches'),
    seller: const LotSellerDto(id: 7, displayName: 'Ana'),
    startingPrice: '100.00',
    minIncrement: '5.00',
    currentPrice: '100.00',
    bidCount: 0,
    hasReserve: false,
    reserveMet: true,
    status: 'DRAFT',
    watched: false,
    canEdit: canEdit,
    canBid: false,
  );
}
