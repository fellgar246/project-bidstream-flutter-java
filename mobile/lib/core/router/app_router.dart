import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'deferred_route.dart';
import '../auth/auth_controller.dart';
import '../auth/auth_state.dart';
import '../../features/auth/presentation/login_screen.dart';
import '../../features/auth/presentation/profile_screen.dart';
import '../../features/auth/presentation/register_screen.dart';
import '../../features/auth/presentation/splash_screen.dart';
import '../../features/categories/presentation/categories_screen.dart';
import '../../features/lots/presentation/live_auction_screen.dart';
import '../../features/lots/presentation/lot_detail_screen.dart';
import '../../features/lots/presentation/lot_edit_screen.dart';
import '../../features/lots/presentation/lot_form_screen.dart';
import '../../features/lots/data/lot_filters.dart';
import '../../features/lots/presentation/lots_list_screen.dart';
import '../../features/lots/presentation/seller_lots_screen.dart';
import '../../features/home/presentation/home_screen.dart';
import '../../features/notifications/presentation/notifications_screen.dart';

final routerProvider = Provider<GoRouter>((ref) {
  final refreshNotifier = ValueNotifier<int>(0);
  ref.listen(authControllerProvider, (_, _) {
    refreshNotifier.value++;
  });

  return GoRouter(
    initialLocation: '/splash',
    refreshListenable: refreshNotifier,
    redirect: (context, state) {
      final authAsync = ref.read(authControllerProvider);
      final location = state.matchedLocation;
      final fullPath = state.uri.path;

      if (authAsync.isLoading || authAsync.hasError) {
        return location == '/splash' ? null : '/splash';
      }

      final status = authAsync.value?.status ?? AuthStatus.unknown;
      switch (status) {
        case AuthStatus.unknown:
          return location == '/splash' ? null : '/splash';
        case AuthStatus.unauthenticated:
          if (_isLotDeepLink(fullPath)) {
            setDeferredRoute(fullPath);
          }
          if (location == '/login' || location == '/register') {
            return null;
          }
          return '/login';
        case AuthStatus.authenticated:
          if (location == '/login' ||
              location == '/register' ||
              location == '/splash') {
            final deferred = takeDeferredRoute();
            if (deferred != null) {
              return deferred;
            }
            return '/';
          }
          return null;
      }
    },
    routes: [
      GoRoute(
        path: '/splash',
        builder: (context, state) => const SplashScreen(),
      ),
      GoRoute(
        path: '/login',
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: '/register',
        builder: (context, state) => const RegisterScreen(),
      ),
      GoRoute(
        path: '/',
        builder: (context, state) => const HomeScreen(),
      ),
      GoRoute(
        path: '/notifications',
        builder: (context, state) => const NotificationsScreen(),
      ),
      GoRoute(
        path: '/categories',
        builder: (context, state) => const CategoriesScreen(),
      ),
      GoRoute(
        path: '/lots',
        builder: (context, state) {
          final query = state.uri.queryParameters;
          final filters = LotFilters(
            query: query['q'],
            categoryId: int.tryParse(query['categoryId'] ?? ''),
            minPriceCents: int.tryParse(query['minPriceCents'] ?? ''),
            maxPriceCents: int.tryParse(query['maxPriceCents'] ?? ''),
          );
          return LotsListScreen(initialFilters: filters);
        },
      ),
      GoRoute(
        path: '/lots/:id/live',
        builder: (context, state) {
          final id = int.parse(state.pathParameters['id']!);
          return LiveAuctionScreen(lotId: id);
        },
      ),
      GoRoute(
        path: '/lots/:id',
        builder: (context, state) {
          final id = int.parse(state.pathParameters['id']!);
          return LotDetailScreen(lotId: id);
        },
      ),
      GoRoute(
        path: '/seller/lots',
        builder: (context, state) => const SellerLotsScreen(),
      ),
      GoRoute(
        path: '/seller/lots/new',
        builder: (context, state) => const LotFormScreen(),
      ),
      GoRoute(
        path: '/seller/lots/:id/edit',
        builder: (context, state) {
          final id = int.parse(state.pathParameters['id']!);
          return LotEditScreen(lotId: id);
        },
      ),
      GoRoute(
        path: '/profile',
        builder: (context, state) => const ProfileScreen(),
      ),
    ],
  );
});

bool _isLotDeepLink(String path) {
  final match = RegExp(r'^/lots/\d+').firstMatch(path);
  return match != null;
}
