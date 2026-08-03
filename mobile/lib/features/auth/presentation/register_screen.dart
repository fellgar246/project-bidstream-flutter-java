import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/auth/auth_controller.dart';
import '../../../core/l10n/app_strings.dart';
import '../../../core/network/api_exception.dart';

class RegisterScreen extends ConsumerStatefulWidget {
  const RegisterScreen({super.key});

  @override
  ConsumerState<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends ConsumerState<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _displayNameController = TextEditingController();

  String? _emailError;
  String? _passwordError;
  String? _displayNameError;
  String? _generalError;
  bool _submitting = false;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _displayNameController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() {
      _emailError = null;
      _passwordError = null;
      _displayNameError = null;
      _generalError = null;
    });

    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() => _submitting = true);
    try {
      await ref.read(authControllerProvider.notifier).register(
            email: _emailController.text.trim(),
            password: _passwordController.text,
            displayName: _displayNameController.text.trim(),
          );
    } on ApiException catch (error) {
      setState(() {
        _emailError = error.details['email'];
        _passwordError = error.details['password'];
        _displayNameError = error.details['displayName'];
        _generalError = _emailError == null &&
                _passwordError == null &&
                _displayNameError == null
            ? error.message
            : null;
      });
    } catch (_) {
      setState(() => _generalError = AppStrings.registerError);
    } finally {
      if (mounted) {
        setState(() => _submitting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text(AppStrings.registerTitle)),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                controller: _displayNameController,
                decoration: InputDecoration(
                  labelText: AppStrings.displayNameLabel,
                  errorText: _displayNameError,
                ),
                validator: (value) {
                  if (value == null || value.trim().isEmpty) {
                    return AppStrings.displayNameRequired;
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _emailController,
                decoration: InputDecoration(
                  labelText: AppStrings.emailLabel,
                  errorText: _emailError,
                ),
                keyboardType: TextInputType.emailAddress,
                validator: (value) {
                  if (value == null || value.trim().isEmpty) {
                    return AppStrings.emailRequired;
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _passwordController,
                decoration: InputDecoration(
                  labelText: AppStrings.passwordLabel,
                  errorText: _passwordError,
                ),
                obscureText: true,
                validator: (value) {
                  if (value == null || value.length < 10) {
                    return AppStrings.passwordMinLength;
                  }
                  return null;
                },
              ),
              if (_generalError != null) ...[
                const SizedBox(height: 16),
                Text(
                  _generalError!,
                  style: TextStyle(color: Theme.of(context).colorScheme.error),
                ),
              ],
              const SizedBox(height: 24),
              FilledButton(
                onPressed: _submitting ? null : _submit,
                child: _submitting
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text(AppStrings.registerAction),
              ),
              TextButton(
                onPressed: () => context.go('/login'),
                child: const Text(AppStrings.goToLogin),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
