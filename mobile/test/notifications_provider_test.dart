import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:bidstream/features/notifications/data/notifications_api.dart';
import 'package:bidstream/features/notifications/providers/notifications_provider.dart';

void main() {
  test('applyPush increments unread count', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);

    expect(container.read(unreadNotificationsCountProvider), 0);

    container
        .read(notificationsProvider.notifier)
        .applyPush(
          const NotificationItem(
            id: 1,
            type: 'OUTBID',
            payload: {'lotId': 10},
            createdAt: '2026-08-03T12:00:00Z',
          ),
        );

    expect(container.read(unreadNotificationsCountProvider), 1);
    expect(container.read(notificationsProvider).items, hasLength(1));
  });
}
