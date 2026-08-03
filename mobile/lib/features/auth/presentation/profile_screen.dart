import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/auth/auth_controller.dart';
import '../../../core/l10n/app_strings.dart';
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
      setState(() => _error = AppStrings.sellerApplicationError);
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

    return Scaffold(
      appBar: AppBar(title: const Text(AppStrings.profileTitle)),
      body: user == null
          ? const Center(child: CircularProgressIndicator())
          : Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(user.displayName, style: Theme.of(context).textTheme.headlineSmall),
                  const SizedBox(height: 8),
                  Text(user.email),
                  const SizedBox(height: 8),
                  Text('${AppStrings.rolesLabel}: ${user.roles.join(', ')}'),
                  if (_error != null) ...[
                    const SizedBox(height: 16),
                    Text(
                      _error!,
                      style: TextStyle(color: Theme.of(context).colorScheme.error),
                    ),
                  ],
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
                          : const Text(AppStrings.becomeSeller),
                    ),
                  const Spacer(),
                  OutlinedButton(
                    onPressed: () =>
                        ref.read(authControllerProvider.notifier).logout(),
                    child: const Text(AppStrings.logoutAction),
                  ),
                ],
              ),
            ),
    );
  }
}
