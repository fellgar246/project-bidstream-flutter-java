import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/auth/token_storage.dart';
import '../../../core/realtime/bidstream_stomp_client.dart';
import '../../../core/realtime/stomp_client.dart';
import '../data/lot_events_api.dart';
import 'lot_detail_provider.dart';

class LiveBidEntry {
  const LiveBidEntry({
    required this.bidId,
    required this.amount,
    required this.bidderDisplayName,
    required this.placedAt,
  });

  final int bidId;
  final String amount;
  final String bidderDisplayName;
  final String placedAt;
}

class LiveAuctionState {
  const LiveAuctionState({
    this.currentPrice = '0.00',
    this.bidCount = 0,
    this.scheduledEndAt,
    this.watching = 0,
    this.showExtendedBanner = false,
    this.bids = const [],
    this.outbidMessage,
    this.closedMessage,
    this.lotStatus,
  });

  final String currentPrice;
  final int bidCount;
  final String? scheduledEndAt;
  final int watching;
  final bool showExtendedBanner;
  final List<LiveBidEntry> bids;
  final String? outbidMessage;
  final String? closedMessage;
  final String? lotStatus;

  LiveAuctionState copyWith({
    String? currentPrice,
    int? bidCount,
    String? scheduledEndAt,
    int? watching,
    bool? showExtendedBanner,
    List<LiveBidEntry>? bids,
    String? outbidMessage,
    String? closedMessage,
    String? lotStatus,
  }) {
    return LiveAuctionState(
      currentPrice: currentPrice ?? this.currentPrice,
      bidCount: bidCount ?? this.bidCount,
      scheduledEndAt: scheduledEndAt ?? this.scheduledEndAt,
      watching: watching ?? this.watching,
      showExtendedBanner: showExtendedBanner ?? this.showExtendedBanner,
      bids: bids ?? this.bids,
      outbidMessage: outbidMessage,
      closedMessage: closedMessage ?? this.closedMessage,
      lotStatus: lotStatus ?? this.lotStatus,
    );
  }
}

final stompConnectionStateProvider =
    StateProvider<StompConnectionState>((ref) => StompConnectionState.disconnected);

final liveAuctionProvider =
    NotifierProvider.family<LiveAuctionNotifier, LiveAuctionState, int>(LiveAuctionNotifier.new);

final bidstreamStompClientProvider = Provider.family<BidstreamStompClient, int>((ref, lotId) {
  final client = BidstreamStompClient(
    readAccessToken: () => ref.read(tokenStorageProvider).readAccessToken(),
    onStateChanged: (state) => ref.read(stompConnectionStateProvider.notifier).state = state,
    onLotEvent: (event) => ref.read(liveAuctionProvider(lotId).notifier).applyEvent(event),
    onPresence: (watching) => ref.read(liveAuctionProvider(lotId).notifier).setWatching(watching),
    onOutbid: (payload) => ref.read(liveAuctionProvider(lotId).notifier).notifyOutbid(payload),
    fetchMissedEvents: (id, afterEventId) =>
        ref.read(lotEventsApiProvider).fetchAfter(id, afterEventId),
    refetchLot: (id) => ref.read(lotDetailProvider(id).notifier).reload(),
  );
  ref.onDispose(client.disconnect);
  return client;
});

class LiveAuctionNotifier extends FamilyNotifier<LiveAuctionState, int> {
  @override
  LiveAuctionState build(int lotId) {
    ref.listen(lotDetailProvider(lotId), (previous, next) {
      next.whenData((lot) {
        state = state.copyWith(
          currentPrice: lot.currentPrice,
          bidCount: lot.bidCount,
          scheduledEndAt: lot.scheduledEndAt,
        );
      });
    });
    return const LiveAuctionState();
  }

  void applyEvent(LotEventMessage event) {
    switch (event.type) {
      case 'BID_PLACED':
        final entry = LiveBidEntry(
          bidId: event.payload['bidId'] as int,
          amount: event.payload['amount'] as String,
          bidderDisplayName: event.payload['bidderDisplayName'] as String,
          placedAt: event.occurredAt,
        );
        state = state.copyWith(
          currentPrice: event.payload['currentPrice'] as String? ?? state.currentPrice,
          bidCount: event.payload['bidCount'] as int? ?? state.bidCount,
          scheduledEndAt: event.payload['scheduledEndAt'] as String? ?? state.scheduledEndAt,
          bids: [entry, ...state.bids].take(20).toList(),
          showExtendedBanner: event.payload['extended'] == true ? true : state.showExtendedBanner,
        );
      case 'LOT_EXTENDED':
        state = state.copyWith(
          scheduledEndAt: event.payload['scheduledEndAt'] as String? ?? state.scheduledEndAt,
          showExtendedBanner: true,
        );
      case 'LOT_CLOSED':
        final status = event.payload['status'] as String? ?? '';
        final reason = event.payload['reason'] as String?;
        final resolvedStatus = status.isNotEmpty
            ? status
            : (reason != null ? 'CLOSED_NO_SALE' : '');
        state = state.copyWith(
          lotStatus: resolvedStatus.isEmpty ? null : resolvedStatus,
        );
      default:
        break;
    }
  }

  void setWatching(int watching) {
    state = state.copyWith(watching: watching);
  }

  void notifyOutbid(Map<String, dynamic> payload) {
    final yourAmount = payload['yourAmount'] as String? ?? '';
    final newAmount = payload['newAmount'] as String? ?? '';
    state = state.copyWith(
      outbidMessage: 'Outbid: your $yourAmount → now $newAmount',
    );
  }
}
