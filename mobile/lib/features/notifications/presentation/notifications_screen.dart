import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/locale_provider.dart';
import '../providers/notifications_provider.dart';

class NotificationsScreen extends ConsumerWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(notificationsProvider);
    final l10n = context.l10n;

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.notificationsTitle),
        actions: [
          TextButton(
            onPressed: state.unreadCount == 0
                ? null
                : () => ref.read(notificationsProvider.notifier).markAllRead(),
            child: Text(l10n.markAllRead),
          ),
        ],
      ),
      body: state.loading && state.items.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : state.items.isEmpty
          ? Center(child: Text(l10n.noNotifications))
          : RefreshIndicator(
              onRefresh: () =>
                  ref.read(notificationsProvider.notifier).refresh(),
              child: ListView.separated(
                itemCount: state.items.length,
                separatorBuilder: (_, __) => const Divider(height: 1),
                itemBuilder: (context, index) {
                  final item = state.items[index];
                  final isUnread = item.readAt == null;
                  return ListTile(
                    leading: Icon(
                      _iconForType(item.type),
                      color: isUnread
                          ? Theme.of(context).colorScheme.primary
                          : null,
                    ),
                    title: Text(
                      _labelForType(context, item.type),
                      style: isUnread
                          ? const TextStyle(fontWeight: FontWeight.bold)
                          : null,
                    ),
                    subtitle: Text(item.createdAt),
                  );
                },
              ),
            ),
    );
  }

  IconData _iconForType(String type) {
    return switch (type) {
      'YOU_WON' => Icons.emoji_events_outlined,
      'OUTBID' => Icons.trending_down,
      'LOT_SOLD' => Icons.sell_outlined,
      _ => Icons.notifications_outlined,
    };
  }

  String _labelForType(BuildContext context, String type) {
    final l10n = context.l10n;
    return switch (type) {
      'YOU_WON' => l10n.notificationYouWon,
      'OUTBID' => l10n.notificationOutbid,
      'LOT_SOLD' => l10n.notificationLotSold,
      'LOT_NO_SALE' => l10n.notificationLotNoSale,
      'LOT_STARTED' => l10n.notificationLotStarted,
      _ => type,
    };
  }
}
