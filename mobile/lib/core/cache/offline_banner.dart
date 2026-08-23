import 'package:flutter/material.dart';

import '../l10n/formatters.dart';
import '../l10n/locale_provider.dart';

class OfflineBanner extends StatelessWidget {
  const OfflineBanner({super.key, required this.cachedAt});

  final DateTime cachedAt;

  @override
  Widget build(BuildContext context) {
    final age = DateTime.now().difference(cachedAt);
    final locale = Localizations.localeOf(context);
    return MaterialBanner(
      content: Text(context.l10n.offlineBanner(formatRelativeAge(age, locale))),
      leading: const Icon(Icons.cloud_off),
      actions: [
        TextButton(
          onPressed: () =>
              ScaffoldMessenger.of(context).hideCurrentMaterialBanner(),
          child: Text(context.l10n.retry),
        ),
      ],
    );
  }
}

class StalePriceBanner extends StatelessWidget {
  const StalePriceBanner({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      color: Colors.orange.shade100,
      padding: const EdgeInsets.all(12),
      child: Row(
        children: [
          const Icon(Icons.warning_amber),
          const SizedBox(width: 8),
          Expanded(child: Text(context.l10n.stalePriceWarning)),
        ],
      ),
    );
  }
}
