import 'package:flutter_test/flutter_test.dart';

import 'package:bidstream/core/network/api_exception.dart';
import 'package:bidstream/features/lots/providers/bid_provider.dart';

void main() {
  group('BidNotifier', () {
    test('mapError_bidTooLow_includesMinimumFromDetails', () {
      final notifier = BidNotifier();
      final message = notifier.minimumBidMessage(
        ApiException(
          code: 'bid_too_low',
          message: 'Bid amount is too low',
          details: const {'minimumCents': '11000'},
        ),
      );
      expect(message, 'Minimum bid is \$110.00');
    });

    test('clientRequestId_reusedAcrossRetries', () {
      const state = BidState(clientRequestId: 'abc-123');
      final reused = state.copyWith(isSubmitting: true);
      expect(reused.clientRequestId, 'abc-123');
    });
  });
}
