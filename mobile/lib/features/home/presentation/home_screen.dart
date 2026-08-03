import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/locale_provider.dart';
import '../../notifications/providers/notifications_provider.dart';
import '../../notifications/providers/notifications_stomp_provider.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.watch(notificationsStompProvider);
    final unread = ref.watch(unreadNotificationsCountProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(context.l10n.homeTitle),
        actions: [
          IconButton(
            onPressed: () => context.go('/notifications'),
            icon: Badge(
              isLabelVisible: unread > 0,
              label: Text('$unread'),
              child: const Icon(Icons.notifications_outlined),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.person_outline),
            onPressed: () => context.go('/profile'),
            tooltip: context.l10n.viewProfile,
          ),
        ],
      ),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              context.l10n.homeSubtitle,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: () => context.go('/categories'),
              child: Text(context.l10n.viewCategories),
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: () => context.go('/lots'),
              child: Text(context.l10n.viewLots),
            ),
            const SizedBox(height: 12),
            OutlinedButton(
              onPressed: () => context.go('/seller/lots'),
              child: Text(context.l10n.viewSellerLots),
            ),
          ],
        ),
      ),
    );
  }
}
