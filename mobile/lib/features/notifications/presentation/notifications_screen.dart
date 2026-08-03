import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/app_strings.dart';
import '../providers/notifications_provider.dart';

class NotificationsScreen extends ConsumerWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(notificationsProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text(AppStrings.notificationsTitle),
        actions: [
          TextButton(
            onPressed: state.unreadCount == 0
                ? null
                : () => ref.read(notificationsProvider.notifier).markAllRead(),
            child: const Text(AppStrings.markAllRead),
          ),
        ],
      ),
      body: state.loading && state.items.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : state.items.isEmpty
              ? const Center(child: Text(AppStrings.noNotifications))
              : RefreshIndicator(
                  onRefresh: () => ref.read(notificationsProvider.notifier).refresh(),
                  child: ListView.separated(
                    itemCount: state.items.length,
                    separatorBuilder: (_, __) => const Divider(height: 1),
                    itemBuilder: (context, index) {
                      final item = state.items[index];
                      final isUnread = item.readAt == null;
                      return ListTile(
                        leading: Icon(
                          _iconForType(item.type),
                          color: isUnread ? Theme.of(context).colorScheme.primary : null,
                        ),
                        title: Text(
                          _labelForType(item.type),
                          style: isUnread ? const TextStyle(fontWeight: FontWeight.bold) : null,
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

  String _labelForType(String type) {
    return switch (type) {
      'YOU_WON' => AppStrings.notificationYouWon,
      'OUTBID' => AppStrings.notificationOutbid,
      'LOT_SOLD' => AppStrings.notificationLotSold,
      'LOT_NO_SALE' => AppStrings.notificationLotNoSale,
      'LOT_STARTED' => AppStrings.notificationLotStarted,
      _ => type,
    };
  }
}
