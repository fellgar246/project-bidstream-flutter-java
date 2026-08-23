import '../../../core/network/dio_client.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_provider.dart';

class NotificationItem {
  const NotificationItem({
    required this.id,
    required this.type,
    required this.payload,
    this.readAt,
    required this.createdAt,
  });

  final int id;
  final String type;
  final Map<String, dynamic> payload;
  final String? readAt;
  final String createdAt;

  factory NotificationItem.fromJson(Map<String, dynamic> json) {
    return NotificationItem(
      id: json['id'] as int,
      type: json['type'] as String,
      payload: Map<String, dynamic>.from(json['payload'] as Map),
      readAt: json['readAt'] as String?,
      createdAt: json['createdAt'] as String,
    );
  }
}

class NotificationsApi {
  NotificationsApi(this._client);

  final DioClient _client;

  Future<({List<NotificationItem> items, int unreadCount})> fetch({
    bool unreadOnly = false,
  }) async {
    final response = await _client.dio.get<Map<String, dynamic>>(
      '/me/notifications',
      queryParameters: {'unreadOnly': unreadOnly},
    );
    final data = response.data!;
    final items = (data['items'] as List)
        .map(
          (e) => NotificationItem.fromJson(Map<String, dynamic>.from(e as Map)),
        )
        .toList();
    return (items: items, unreadCount: data['unreadCount'] as int);
  }

  Future<void> markRead(int id) async {
    await _client.dio.post<void>('/me/notifications/$id/read');
  }

  Future<void> markAllRead() async {
    await _client.dio.post<void>('/me/notifications/read-all');
  }
}

final notificationsApiProvider = Provider<NotificationsApi>((ref) {
  return NotificationsApi(ref.watch(dioClientProvider));
});
