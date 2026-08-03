import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/data/auth_api.dart';
import '../../features/auth/data/auth_dto.dart';
import '../network/dio_provider.dart';
import 'auth_state.dart';
import 'token_storage.dart';

final authApiProvider = Provider<AuthApi>((ref) {
  return AuthApi(ref.watch(dioClientProvider).dio);
});

class AuthController extends AsyncNotifier<AuthState> {
  @override
  Future<AuthState> build() => _restoreSession();

  Future<AuthState> _restoreSession() async {
    final storage = ref.read(tokenStorageProvider);
    final accessToken = await storage.readAccessToken();
    final refreshToken = await storage.readRefreshToken();

    if (accessToken == null || refreshToken == null) {
      return const AuthState.unauthenticated();
    }

    try {
      final user = await ref.read(authApiProvider).me();
      return AuthState.authenticated(_toSnapshot(user));
    } catch (_) {
      try {
        final tokens = await ref.read(authApiProvider).refresh(refreshToken: refreshToken);
        await storage.saveTokens(
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
        );
        return AuthState.authenticated(_toSnapshot(tokens.user));
      } catch (_) {
        await storage.clear();
        return const AuthState.unauthenticated();
      }
    }
  }

  Future<void> login({required String email, required String password}) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final tokens = await ref.read(authApiProvider).login(email: email, password: password);
      await _persist(tokens);
      return AuthState.authenticated(_toSnapshot(tokens.user));
    });
  }

  Future<void> register({
    required String email,
    required String password,
    required String displayName,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(authApiProvider).register(
            email: email,
            password: password,
            displayName: displayName,
          );
      final tokens = await ref.read(authApiProvider).login(email: email, password: password);
      await _persist(tokens);
      return AuthState.authenticated(_toSnapshot(tokens.user));
    });
  }

  Future<void> logout() async {
    final refreshToken = await ref.read(tokenStorageProvider).readRefreshToken();
    if (refreshToken != null) {
      try {
        await ref.read(authApiProvider).logout(refreshToken: refreshToken);
      } catch (_) {
        // Best effort — local session is cleared regardless.
      }
    }
    await ref.read(tokenStorageProvider).clear();
    state = const AsyncData(AuthState.unauthenticated());
  }

  Future<void> applySeller() async {
    final current = state.valueOrNull;
    if (current?.user == null) {
      return;
    }
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final tokens = await ref.read(authApiProvider).applySeller();
      await _persist(tokens);
      return AuthState.authenticated(_toSnapshot(tokens.user));
    });
  }

  Future<void> onSessionExpired() async {
    await ref.read(tokenStorageProvider).clear();
    state = const AsyncData(AuthState.unauthenticated());
  }

  Future<void> replaceTokens(AuthTokensDto tokens) async {
    await _persist(tokens);
    state = AsyncData(AuthState.authenticated(_toSnapshot(tokens.user)));
  }

  Future<void> _persist(AuthTokensDto tokens) async {
    await ref.read(tokenStorageProvider).saveTokens(
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
        );
  }

  UserSnapshot _toSnapshot(UserDto user) {
    return UserSnapshot(
      id: user.id,
      email: user.email,
      displayName: user.displayName,
      roles: user.roles,
    );
  }
}

final authControllerProvider =
    AsyncNotifierProvider<AuthController, AuthState>(AuthController.new);
