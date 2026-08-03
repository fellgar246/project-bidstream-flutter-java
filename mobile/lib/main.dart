import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/config/app_config.dart';
import 'core/auth/auth_state.dart';
import 'core/auth/auth_controller.dart';
import 'core/cache/database_provider.dart';
import 'core/cache/lot_cache_service.dart';
import 'core/l10n/locale_provider.dart';
import 'core/push/push_service.dart';
import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'l10n/app_localizations.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    await Firebase.initializeApp();
  } catch (_) {}
  final prefs = await SharedPreferences.getInstance();
  final database = await openAppDatabase();
  runApp(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        appDatabaseProvider.overrideWithValue(database),
        lotCacheServiceProvider.overrideWithValue(LotCacheService(database)),
      ],
      child: const BidstreamApp(),
    ),
  );
}

class BidstreamApp extends ConsumerStatefulWidget {
  const BidstreamApp({super.key});

  @override
  ConsumerState<BidstreamApp> createState() => _BidstreamAppState();
}

class _BidstreamAppState extends ConsumerState<BidstreamApp> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      final router = ref.read(routerProvider);
      ref.read(pushServiceProvider).initialize(
            onTap: (deepLink) {
              final auth = ref.read(authControllerProvider).valueOrNull;
              handleDeepLink(
                router,
                deepLink,
                isAuthenticated: auth?.status == AuthStatus.authenticated,
              );
            },
          );
    });
  }

  @override
  Widget build(BuildContext context) {
    final router = ref.watch(routerProvider);
    final locale = ref.watch(localeProvider);
    return MaterialApp.router(
      scaffoldMessengerKey: rootScaffoldMessengerKey,
      title: AppConfig.appName,
      theme: AppTheme.light(),
      locale: locale,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: AppLocalizations.supportedLocales,
      routerConfig: router,
    );
  }
}
