import 'package:bidstream/core/auth/auth_controller.dart';
import 'package:bidstream/core/auth/auth_state.dart';
import 'package:bidstream/core/network/api_exception.dart';
import 'package:bidstream/features/auth/presentation/login_screen.dart';
import 'package:bidstream/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'l10n_test_helper.dart';

void main() {
  final en = lookupAppLocalizations(const Locale('en'));

  testWidgets('login screen maps field errors from ApiException details', (
    tester,
  ) async {
    await pumpLocalized(
      tester,
      ProviderScope(
        overrides: [
          authControllerProvider.overrideWith(_FailingLoginAuthController.new),
        ],
        child: const LoginScreen(),
      ),
      locale: const Locale('en'),
    );

    await tester.enterText(
      find.byKey(const Key('login_email')),
      'bad@example.com',
    );
    await tester.enterText(find.byKey(const Key('login_password')), 'short');
    await tester.tap(find.widgetWithText(FilledButton, en.loginAction));
    await tester.pumpAndSettle();

    expect(find.text('Invalid email format'), findsOneWidget);
    expect(
      find.text('Password must be at least 10 characters'),
      findsOneWidget,
    );
  });
}

class _FailingLoginAuthController extends AuthController {
  @override
  Future<AuthState> build() async => const AuthState.unauthenticated();

  @override
  Future<void> login({required String email, required String password}) async {
    throw ApiException(
      code: 'validation_error',
      message: 'Validation failed',
      details: const {
        'email': 'Invalid email format',
        'password': 'Password must be at least 10 characters',
      },
    );
  }
}
