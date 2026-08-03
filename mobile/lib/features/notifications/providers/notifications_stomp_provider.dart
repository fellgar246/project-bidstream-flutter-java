import 'dart:async';
import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../../../core/auth/token_storage.dart';
import '../../../core/realtime/bidstream_stomp_client.dart';
import '../data/notifications_api.dart';
import 'notifications_provider.dart';

final notificationsStompProvider = Provider<void>((ref) {
  final tokenStorage = ref.watch(tokenStorageProvider);
  StompClient? client;

  Future<void> connect() async {
    final token = await tokenStorage.readAccessToken();
    if (token == null || token.isEmpty) {
      return;
    }
    client?.deactivate();
    client = StompClient(
      config: StompConfig(
        url: '${wsBaseUrl()}?token=$token',
        onConnect: (_) {
          client?.subscribe(
            destination: '/user/queue/notifications',
            callback: (frame) {
              final body = frame.body;
              if (body == null) {
                return;
              }
              final json = Map<String, dynamic>.from(jsonDecode(body) as Map);
              ref.read(notificationsProvider.notifier).applyPush(
                    NotificationItem.fromJson(json),
                  );
            },
          );
        },
        reconnectDelay: const Duration(seconds: 5),
        heartbeatIncoming: const Duration(seconds: 10),
        heartbeatOutgoing: const Duration(seconds: 10),
      ),
    );
    client!.activate();
  }

  unawaited(connect());
  ref.onDispose(() => client?.deactivate());
});
