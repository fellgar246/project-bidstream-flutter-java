import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/auth/auth_controller.dart';
import '../../../core/cache/lot_cache_service.dart';
import '../../../core/l10n/locale_provider.dart';
import '../../../core/network/api_exception.dart';

class ProfileScreen extends ConsumerStatefulWidget {
  const ProfileScreen({super.key});

  @override
  ConsumerState<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends ConsumerState<ProfileScreen> {
  bool _applyingSeller = false;
  String? _error;

  Future<void> _applySeller() async {
    setState(() {
      _applyingSeller = true;
      _error = null;
    });
    try {
      await ref.read(authControllerProvider.notifier).applySeller();
    } on ApiException catch (error) {
      setState(() => _error = error.message);
    } catch (_) {
      if (mounted) {
        setState(() => _error = context.l10n.sellerApplicationError);
      }
    } finally {
      if (mounted) {
        setState(() => _applyingSeller = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authControllerProvider).valueOrNull;
    final user = authState?.user;
    final locale = ref.watch(localeProvider);
    final l10n = context.l10n;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.profileTitle)),
      body: user == null
          ? const Center(child: CircularProgressIndicator())
          : Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    user.displayName,
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 8),
                  Text(user.email),
                  const SizedBox(height: 8),
                  Text('${l10n.rolesLabel}: ${user.roles.join(', ')}'),
                  if (_error != null) ...[
                    const SizedBox(height: 16),
                    Text(
                      _error!,
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.error,
                      ),
                    ),
                  ],
                  const SizedBox(height: 24),
                  Text(
                    l10n.languageLabel,
                    style: Theme.of(context).textTheme.titleSmall,
                  ),
                  const SizedBox(height: 8),
                  DropdownButtonFormField<Locale?>(
                    initialValue: locale,
                    decoration: const InputDecoration(
                      border: OutlineInputBorder(),
                    ),
                    items: [
                      DropdownMenuItem(
                        value: null,
                        child: Text(l10n.languageSystem),
                      ),
                      DropdownMenuItem(
                        value: const Locale('es'),
                        child: Text(l10n.languageSpanish),
                      ),
                      DropdownMenuItem(
                        value: const Locale('en'),
                        child: Text(l10n.languageEnglish),
                      ),
                    ],
                    onChanged: (value) =>
                        ref.read(localeProvider.notifier).setLocale(value),
                  ),
                  const SizedBox(height: 24),
                  if (!user.isSeller)
                    FilledButton(
                      onPressed: _applyingSeller ? null : _applySeller,
                      child: _applyingSeller
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : Text(l10n.becomeSeller),
                    ),
                  const Spacer(),
                  OutlinedButton(
                    onPressed: () async {
                      await ref.read(lotCacheServiceProvider).clearAll();
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text(l10n.cacheCleared)),
                        );
                      }
                    },
                    child: Text(l10n.clearCache),
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton(
                    onPressed: () =>
                        ref.read(authControllerProvider.notifier).logout(),
                    child: Text(l10n.logoutAction),
                  ),
                ],
              ),
            ),
    );
  }
}
