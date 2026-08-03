import 'package:bidstream/core/realtime/stomp_client.dart';
import 'package:bidstream/features/lots/providers/live_auction_provider.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('ca0612_applyEvent_showsExtendedBanner', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);
    final notifier = container.read(liveAuctionProvider(1).notifier);
    notifier.applyEvent(
      LotEventMessage(
        eventId: 3,
        type: 'LOT_EXTENDED',
        lotId: 1,
        occurredAt: '2026-08-01T18:30:00Z',
        payload: {'scheduledEndAt': '2026-08-01T19:00:00Z'},
      ),
    );
    final state = container.read(liveAuctionProvider(1));
    expect(state.showExtendedBanner, isTrue);
    expect(state.scheduledEndAt, '2026-08-01T19:00:00Z');
  });

  test('ca0612_applyBidEvent_updatesPriceAndBidList', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);
    final notifier = container.read(liveAuctionProvider(1).notifier);
    notifier.applyEvent(
      LotEventMessage(
        eventId: 2,
        type: 'BID_PLACED',
        lotId: 1,
        occurredAt: '2026-08-01T18:30:00Z',
        payload: {
          'bidId': 5,
          'amount': '120.00',
          'bidderDisplayName': 'Ana',
          'currentPrice': '120.00',
          'bidCount': 2,
          'scheduledEndAt': '2026-08-01T19:00:00Z',
          'extended': false,
        },
      ),
    );
    final state = container.read(liveAuctionProvider(1));
    expect(state.currentPrice, '120.00');
    expect(state.bids.single.bidderDisplayName, 'Ana');
  });
}
