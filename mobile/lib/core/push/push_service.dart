import 'dart:io';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/push/data/devices_api.dart';
import '../router/deferred_route.dart';

typedef PushTapHandler = void Function(String deepLink);

final pushServiceProvider = Provider<PushService>((ref) {
  return PushService(ref);
});

class PushService {
  PushService(this._ref);

  final Ref _ref;
  String? _currentToken;
  PushTapHandler? _onTap;

  Future<void> initialize({required PushTapHandler onTap}) async {
    _onTap = onTap;
    if (kIsWeb) {
      return;
    }
    try {
      final messaging = FirebaseMessaging.instance;
      await messaging.requestPermission();
      final token = await messaging.getToken();
      if (token != null) {
        await _registerToken(token);
      }
      messaging.onTokenRefresh.listen(_registerToken);
      FirebaseMessaging.onMessage.listen(_showForegroundBanner);
      FirebaseMessaging.onMessageOpenedApp.listen(_handleMessage);
      final initial = await messaging.getInitialMessage();
      if (initial != null) {
        _handleMessage(initial);
      }
    } catch (_) {
      // Firebase may be unavailable in tests/dev without google-services.json.
    }
  }

  Future<void> _registerToken(String token) async {
    _currentToken = token;
    try {
      await _ref
          .read(devicesApiProvider)
          .register(token: token, platform: Platform.isIOS ? 'ios' : 'android');
    } catch (_) {}
  }

  Future<void> unregisterOnLogout() async {
    final token = _currentToken;
    if (token == null) {
      return;
    }
    try {
      await _ref.read(devicesApiProvider).unregister(token);
    } catch (_) {}
    _currentToken = null;
  }

  void _showForegroundBanner(RemoteMessage message) {
    final deepLink = message.data['deepLink'];
    final title = message.notification?.title ?? message.data['title'] ?? '';
    final body = message.notification?.body ?? message.data['body'] ?? '';
    final messenger = rootScaffoldMessengerKey.currentState;
    if (messenger == null || deepLink == null) {
      return;
    }
    messenger.showSnackBar(
      SnackBar(
        content: Text('$title — $body'),
        action: SnackBarAction(
          label: 'Open',
          onPressed: () => _onTap?.call(deepLink),
        ),
      ),
    );
  }

  void _handleMessage(RemoteMessage message) {
    final deepLink = message.data['deepLink'];
    if (deepLink != null) {
      _onTap?.call(deepLink);
    }
  }

  void handleSimulatedMessage(Map<String, String> data) {
    final deepLink = data['deepLink'];
    if (deepLink != null) {
      _onTap?.call(deepLink);
    }
  }
}

final rootScaffoldMessengerKey = GlobalKey<ScaffoldMessengerState>();

void handleDeepLink(
  GoRouter router,
  String link, {
  required bool isAuthenticated,
}) {
  final uri = Uri.parse(link);
  final path = _pathFromUri(uri);
  if (path == null) {
    return;
  }
  if (isAuthenticated) {
    router.go(path);
  } else {
    setDeferredRoute(path);
    router.go('/login');
  }
}

String? _pathFromUri(Uri uri) {
  if (uri.scheme == 'bidstream' && uri.host == 'lots') {
    final id = uri.pathSegments.isNotEmpty ? uri.pathSegments.first : null;
    if (id != null) return '/lots/$id';
  }
  if (uri.scheme == 'https' && uri.host == 'bidstream.app') {
    final segments = uri.pathSegments;
    if (segments.length >= 2 && segments[0] == 'lots') {
      return '/lots/${segments[1]}';
    }
  }
  if (uri.path.startsWith('/lots/')) {
    return uri.path;
  }
  return null;
}
