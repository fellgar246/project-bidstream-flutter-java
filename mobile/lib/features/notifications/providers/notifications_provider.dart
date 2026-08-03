import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/notifications_api.dart';

class NotificationsState {
  const NotificationsState({
    this.items = const [],
    this.unreadCount = 0,
    this.loading = false,
  });

  final List<NotificationItem> items;
  final int unreadCount;
  final bool loading;

  NotificationsState copyWith({
    List<NotificationItem>? items,
    int? unreadCount,
    bool? loading,
  }) {
    return NotificationsState(
      items: items ?? this.items,
      unreadCount: unreadCount ?? this.unreadCount,
      loading: loading ?? this.loading,
    );
  }
}

final notificationsProvider =
    NotifierProvider<NotificationsNotifier, NotificationsState>(NotificationsNotifier.new);

class NotificationsNotifier extends Notifier<NotificationsState> {
  @override
  NotificationsState build() {
    Future.microtask(refresh);
    return const NotificationsState(loading: true);
  }

  Future<void> refresh() async {
    state = state.copyWith(loading: true);
    try {
      final result = await ref.read(notificationsApiProvider).fetch();
      state = NotificationsState(items: result.items, unreadCount: result.unreadCount);
    } finally {
      state = state.copyWith(loading: false);
    }
  }

  void applyPush(NotificationItem item) {
    state = state.copyWith(
      items: [item, ...state.items],
      unreadCount: state.unreadCount + 1,
    );
  }

  Future<void> markAllRead() async {
    await ref.read(notificationsApiProvider).markAllRead();
    state = state.copyWith(unreadCount: 0);
    await refresh();
  }
}

final unreadNotificationsCountProvider = Provider<int>((ref) {
  return ref.watch(notificationsProvider).unreadCount;
});
