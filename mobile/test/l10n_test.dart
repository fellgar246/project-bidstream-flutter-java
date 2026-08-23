import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:bidstream/core/l10n/formatters.dart';
import 'package:bidstream/l10n/app_localizations.dart';

import 'l10n_test_helper.dart';

void main() {
  group('bid count plurals', () {
    testWidgets('spanish plurals', (tester) async {
      late AppLocalizations l10n;
      await tester.pumpWidget(
        buildLocalized(
          Builder(
            builder: (context) {
              l10n = AppLocalizations.of(context);
              return const SizedBox.shrink();
            },
          ),
          locale: const Locale('es'),
        ),
      );
      expect(l10n.bidCount(0), 'Sin pujas');
      expect(l10n.bidCount(1), '1 puja');
      expect(l10n.bidCount(7), '7 pujas');
    });

    testWidgets('english plurals', (tester) async {
      late AppLocalizations l10n;
      await tester.pumpWidget(
        buildLocalized(
          Builder(
            builder: (context) {
              l10n = AppLocalizations.of(context);
              return const SizedBox.shrink();
            },
          ),
          locale: const Locale('en'),
        ),
      );
      expect(l10n.bidCount(0), 'No bids');
      expect(l10n.bidCount(1), '1 bid');
      expect(l10n.bidCount(7), '7 bids');
    });
  });

  group('currency formatting', () {
    test('formats USD in en locale', () {
      final formatted = formatMoney(
        Decimal.parse('1250.00'),
        const Locale('en', 'US'),
      );
      expect(formatted, contains('1,250.00'));
      expect(formatted, contains(r'$'));
    });

    test('formats in es locale', () {
      final formatted = formatMoney(
        Decimal.parse('1250.00'),
        const Locale('es', 'ES'),
      );
      expect(formatted, contains('1.250,00'));
      expect(formatted, contains('€'));
    });
  });
}
