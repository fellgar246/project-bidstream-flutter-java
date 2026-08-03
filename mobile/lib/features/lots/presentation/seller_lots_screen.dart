import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/locale_provider.dart';
import '../data/lot_dto.dart';
import '../providers/lots_list_provider.dart';

final sellerLotsProvider = FutureProvider<List<LotDto>>((ref) {
  return ref.watch(lotsApiProvider).fetchMyLots();
});

class SellerLotsScreen extends ConsumerWidget {
  const SellerLotsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final lotsAsync = ref.watch(sellerLotsProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(context.l10n.sellerLotsTitle),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => context.go('/seller/lots/new'),
          ),
        ],
      ),
      body: lotsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(context.l10n.lotsError),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: () => ref.invalidate(sellerLotsProvider),
                child: Text(context.l10n.retry),
              ),
            ],
          ),
        ),
        data: (lots) {
          if (lots.isEmpty) {
            return Center(
              child: FilledButton(
                onPressed: () => context.go('/seller/lots/new'),
                child: Text(context.l10n.lotCreateAction),
              ),
            );
          }

          final grouped = <String, List<LotDto>>{};
          for (final lot in lots) {
            grouped.putIfAbsent(lot.status, () => []).add(lot);
          }

          return ListView(
            children: grouped.entries.map((entry) {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                    child: Text(
                      entry.key,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ),
                  ...entry.value.map(
                    (lot) => ListTile(
                      title: Text(lot.title),
                      subtitle: Text(lot.currentPrice),
                      onTap: () => context.go('/lots/${lot.id}'),
                    ),
                  ),
                ],
              );
            }).toList(),
          );
        },
      ),
    );
  }
}
