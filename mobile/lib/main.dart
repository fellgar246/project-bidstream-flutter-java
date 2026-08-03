import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/l10n/app_strings.dart';
import '../../core/router/app_router.dart';
import '../../core/theme/app_theme.dart';

void main() {
  runApp(const ProviderScope(child: BidstreamApp()));
}

class BidstreamApp extends ConsumerWidget {
  const BidstreamApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);
    return MaterialApp.router(
      title: AppStrings.appTitle,
      theme: AppTheme.light(),
      routerConfig: router,
    );
  }
}
