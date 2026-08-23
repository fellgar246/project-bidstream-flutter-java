import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:bidstream/core/push/push_service.dart';
import 'package:bidstream/core/router/deferred_route.dart';

void main() {
  test('bidstream scheme resolves to lot path', () {
    final router = GoRouter(
      routes: [GoRoute(path: '/', builder: (_, _) => const SizedBox.shrink())],
    );
    handleDeepLink(router, 'bidstream://lots/42', isAuthenticated: true);
    expect(router.routeInformationProvider.value.uri.path, '/lots/42');
  });

  test('https app link resolves to lot path', () {
    final router = GoRouter(
      routes: [GoRoute(path: '/', builder: (_, _) => const SizedBox.shrink())],
    );
    handleDeepLink(
      router,
      'https://bidstream.app/lots/42',
      isAuthenticated: true,
    );
    expect(router.routeInformationProvider.value.uri.path, '/lots/42');
  });

  test('unauthenticated deep link stores deferred route', () {
    final router = GoRouter(
      initialLocation: '/',
      routes: [
        GoRoute(path: '/', builder: (_, _) => const SizedBox.shrink()),
        GoRoute(path: '/login', builder: (_, _) => const SizedBox.shrink()),
      ],
    );
    handleDeepLink(router, 'bidstream://lots/42', isAuthenticated: false);
    expect(peekDeferredRoute(), '/lots/42');
    expect(router.routeInformationProvider.value.uri.path, '/login');
    takeDeferredRoute();
  });
}
