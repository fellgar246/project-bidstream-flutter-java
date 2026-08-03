import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../auth/auth_controller.dart';
import '../auth/auth_interceptor.dart';
import '../auth/token_storage.dart';
import '../../features/auth/data/auth_dto.dart';
import 'dio_client.dart';

final dioClientProvider = Provider<DioClient>((ref) {
  final tokenStorage = ref.watch(tokenStorageProvider);
  final client = DioClient();

  client.dio.interceptors.insert(
    0,
    AuthInterceptor(
      dio: client.dio,
      tokenStorage: tokenStorage,
      refreshTokens: (refreshToken) async {
        final refreshDio = Dio(
          BaseOptions(
            baseUrl: client.dio.options.baseUrl,
            headers: const {'Accept': 'application/json'},
          ),
        );
        final response = await refreshDio.post<Map<String, dynamic>>(
          '/auth/refresh',
          data: {'refreshToken': refreshToken},
        );
        return AuthTokensDto.fromJson(response.data!);
      },
      onSessionExpired: () {
        ref.read(authControllerProvider.notifier).onSessionExpired();
      },
    ),
  );
  return client;
});
