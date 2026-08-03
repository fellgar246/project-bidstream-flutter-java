import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/app_strings.dart';
import '../providers/lot_detail_provider.dart';

class LotDetailScreen extends ConsumerWidget {
  const LotDetailScreen({super.key, required this.lotId});

  final int lotId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final lotAsync = ref.watch(lotDetailProvider(lotId));

    return Scaffold(
      appBar: AppBar(title: const Text(AppStrings.lotDetailTitle)),
      body: lotAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(AppStrings.lotsError),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: () => ref.read(lotDetailProvider(lotId).notifier).reload(),
                child: const Text(AppStrings.retry),
              ),
            ],
          ),
        ),
        data: (lot) => ListView(
          padding: const EdgeInsets.all(24),
          children: [
            Text(lot.title, style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 8),
            Text(lot.description),
            const SizedBox(height: 16),
            Text('${AppStrings.lotCurrentPrice}: ${lot.currentPrice}'),
            Text('${AppStrings.lotStatus}: ${lot.status}'),
            Text('${AppStrings.lotSeller}: ${lot.seller.displayName}'),
            const SizedBox(height: 24),
            if (lot.canEdit)
              FilledButton(
                onPressed: () => context.go('/seller/lots/${lot.id}/edit'),
                child: const Text(AppStrings.lotEdit),
              ),
          ],
        ),
      ),
    );
  }
}
