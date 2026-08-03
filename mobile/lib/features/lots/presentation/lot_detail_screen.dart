import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/cache/offline_banner.dart';
import '../../../core/l10n/locale_provider.dart';
import '../presentation/bid_history_chart.dart';
import '../providers/bid_provider.dart';
import '../providers/lot_detail_provider.dart';
import 'bid_bottom_sheet.dart';

class LotDetailScreen extends ConsumerWidget {
  const LotDetailScreen({super.key, required this.lotId});

  final int lotId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final lotAsync = ref.watch(lotDetailProvider(lotId));
    final l10n = context.l10n;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.lotDetailTitle)),
      body: lotAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(l10n.lotsError),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: () => ref.read(lotDetailProvider(lotId).notifier).reload(),
                child: Text(l10n.retry),
              ),
            ],
          ),
        ),
        data: (detail) {
          final lot = detail.lot;
          return ListView(
          padding: const EdgeInsets.all(24),
          children: [
            if (detail.offline && detail.cachedAt != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: OfflineBanner(cachedAt: detail.cachedAt!),
              ),
            if (detail.stalePrice) const StalePriceBanner(),
            Text(lot.title, style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 8),
            if (lot.images.isNotEmpty) ...[
              Hero(
                tag: 'lot-cover-${lot.id}',
                child: SizedBox(
                  height: 120,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: lot.images.length,
                    separatorBuilder: (_, _) => const SizedBox(width: 8),
                    itemBuilder: (context, index) {
                      final image = lot.images[index];
                      final url = image.thumbnailUrl ?? image.url;
                      return ClipRRect(
                        borderRadius: BorderRadius.circular(12),
                        child: url == null
                            ? const SizedBox(
                                width: 120,
                                height: 120,
                                child: ColoredBox(color: Colors.black12),
                              )
                            : Image.network(url, width: 120, height: 120, fit: BoxFit.cover),
                      );
                    },
                  ),
                ),
              ),
              const SizedBox(height: 16),
            ],
            if (lot.status.startsWith('CLOSED') && lot.bidCount > 0) ...[
              BidHistoryChart(
                amounts: List.generate(
                  lot.bidCount.clamp(0, 500),
                  (index) => lot.currentPrice,
                ),
              ),
              const SizedBox(height: 16),
            ],
            Text(lot.description),
            const SizedBox(height: 16),
            Text('${l10n.lotCurrentPrice}: ${lot.currentPrice}'),
            Text('${l10n.lotStatus}: ${lot.status}'),
            Text('${l10n.lotSeller}: ${lot.seller.displayName}'),
            const SizedBox(height: 24),
            if (lot.canEdit)
              FilledButton(
                onPressed: () => context.go('/seller/lots/${lot.id}/edit'),
                child: Text(l10n.lotEdit),
              ),
            if (lot.canBid) ...[
              const SizedBox(height: 12),
              FilledButton(
                onPressed: () => context.go('/lots/${lot.id}/live'),
                child: Text(l10n.liveWatchAction),
              ),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: () async {
                  ref.read(bidProvider(lot.id).notifier).resetAttempt();
                  await showModalBottomSheet<bool>(
                    context: context,
                    isScrollControlled: true,
                    builder: (_) => BidBottomSheet(lot: lot),
                  );
                },
                child: Text(l10n.bidPlaceAction),
              ),
            ],
          ],
        );
        },
      ),
    );
  }
}
