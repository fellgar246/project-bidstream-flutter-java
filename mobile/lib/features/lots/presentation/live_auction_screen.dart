import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/locale_provider.dart';
import '../../../core/realtime/stomp_client.dart';
import '../presentation/countdown_arc.dart';
import '../providers/live_auction_provider.dart';
import '../providers/lot_detail_provider.dart';

class LiveAuctionScreen extends ConsumerStatefulWidget {
  const LiveAuctionScreen({super.key, required this.lotId});

  final int lotId;

  @override
  ConsumerState<LiveAuctionScreen> createState() => _LiveAuctionScreenState();
}

class _LiveAuctionScreenState extends ConsumerState<LiveAuctionScreen> {
  final GlobalKey<AnimatedListState> _bidListKey =
      GlobalKey<AnimatedListState>();
  Timer? _ticker;
  Duration _remaining = Duration.zero;
  double _displayPrice = 0;
  bool _priceFlash = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref
          .read(bidstreamStompClientProvider(widget.lotId))
          .connectToLot(widget.lotId);
    });
    _ticker = Timer.periodic(
      const Duration(seconds: 1),
      (_) => _recalculateCountdown(),
    );
  }

  @override
  void dispose() {
    _ticker?.cancel();
    super.dispose();
  }

  void _recalculateCountdown() {
    final endRaw = ref.read(liveAuctionProvider(widget.lotId)).scheduledEndAt;
    if (endRaw == null) {
      return;
    }
    final end = DateTime.parse(endRaw).toUtc();
    final now = DateTime.now().toUtc();
    setState(() {
      _remaining = end.isAfter(now) ? end.difference(now) : Duration.zero;
    });
  }

  @override
  Widget build(BuildContext context) {
    final lotAsync = ref.watch(lotDetailProvider(widget.lotId));
    final live = ref.watch(liveAuctionProvider(widget.lotId));
    final connection = ref.watch(stompConnectionStateProvider);
    final l10n = context.l10n;

    ref.listen(liveAuctionProvider(widget.lotId), (previous, next) {
      final parsed =
          double.tryParse(next.currentPrice.replaceAll(',', '')) ??
          _displayPrice;
      if (parsed != _displayPrice) {
        setState(() {
          _displayPrice = parsed;
          _priceFlash = true;
        });
        Future.delayed(const Duration(milliseconds: 400), () {
          if (mounted) {
            setState(() => _priceFlash = false);
          }
        });
      }
      _recalculateCountdown();
    });

    return Scaffold(
      appBar: AppBar(
        title: lotAsync.maybeWhen(
          data: (detail) => Text(detail.lot.title),
          orElse: () => Text(l10n.liveAuctionTitle),
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Center(child: Text('👀 ${live.watching}')),
          ),
        ],
      ),
      body: lotAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, _) => Center(child: Text(l10n.lotsError)),
        data: (_) => Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _ConnectionBanner(connection: connection),
            if (live.showExtendedBanner)
              Container(
                color: Colors.amber.shade100,
                padding: const EdgeInsets.all(12),
                child: Text('⏱ ${l10n.liveExtendedBanner}'),
              ),
            if (live.outbidMessage != null)
              Container(
                color: Colors.red.shade50,
                padding: const EdgeInsets.all(12),
                child: Text(live.outbidMessage!),
              ),
            if (live.lotStatus == 'CLOSED_SOLD')
              Container(
                color: Colors.green.shade50,
                padding: const EdgeInsets.all(12),
                child: Text(
                  l10n.liveYouWon,
                  style: const TextStyle(fontSize: 16),
                ),
              )
            else if (live.lotStatus == 'CLOSED_NO_SALE')
              Container(
                color: Colors.green.shade50,
                padding: const EdgeInsets.all(12),
                child: Text(
                  l10n.liveClosedNoSale,
                  style: const TextStyle(fontSize: 16),
                ),
              ),
            Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                children: [
                  AnimatedContainer(
                    duration: const Duration(milliseconds: 300),
                    decoration: BoxDecoration(
                      color: _priceFlash
                          ? Colors.green.shade100
                          : Colors.transparent,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    padding: const EdgeInsets.all(12),
                    child: TweenAnimationBuilder<double>(
                      tween: Tween<double>(
                        begin: _displayPrice,
                        end:
                            double.tryParse(
                              live.currentPrice.replaceAll(',', ''),
                            ) ??
                            _displayPrice,
                      ),
                      duration: const Duration(milliseconds: 400),
                      builder: (context, value, _) {
                        return Text(
                          '\$${value.toStringAsFixed(2)}',
                          style: Theme.of(context).textTheme.displaySmall,
                        );
                      },
                    ),
                  ),
                  const SizedBox(height: 8),
                  CountdownArc(
                    remaining: _remaining,
                    total: const Duration(minutes: 30),
                    label: _formatDuration(_remaining),
                  ),
                  const SizedBox(height: 8),
                  Text(l10n.bidCount(live.bidCount)),
                ],
              ),
            ),
            Expanded(
              child: AnimatedList(
                key: _bidListKey,
                initialItemCount: live.bids.length,
                itemBuilder: (context, index, animation) {
                  final bid = live.bids[index];
                  return SizeTransition(
                    sizeFactor: animation,
                    child: ListTile(
                      title: Text(bid.bidderDisplayName),
                      subtitle: Text(bid.placedAt),
                      trailing: Text('\$${bid.amount}'),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatDuration(Duration duration) {
    final hours = duration.inHours;
    final minutes = duration.inMinutes.remainder(60);
    final seconds = duration.inSeconds.remainder(60);
    if (hours > 0) {
      return '${hours}h ${minutes}m ${seconds}s';
    }
    return '${minutes}m ${seconds}s';
  }
}

class _ConnectionBanner extends StatelessWidget {
  const _ConnectionBanner({required this.connection});

  final StompConnectionState connection;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return switch (connection) {
      StompConnectionState.connected => const SizedBox.shrink(),
      StompConnectionState.connecting => Container(
        color: Colors.orange.shade100,
        padding: const EdgeInsets.all(8),
        child: Text(l10n.liveReconnecting),
      ),
      StompConnectionState.disconnected => Container(
        color: Colors.grey.shade300,
        padding: const EdgeInsets.all(8),
        child: Text(l10n.liveDisconnected),
      ),
    };
  }
}
