import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:bidstream/core/auth/auth_interceptor.dart';
import 'package:bidstream/core/auth/token_storage.dart';
import 'package:bidstream/features/auth/data/auth_dto.dart';

void main() {
  test('concurrent 401 responses trigger a single refresh call', () async {
    var refreshCallCount = 0;
    final tokenStorage = _InMemoryTokenStorage(
      accessToken: 'expired-access',
      refreshToken: 'valid-refresh',
    );

    final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
    dio.httpClientAdapter = _StatusCodeAdapter();

    dio.interceptors.add(
      AuthInterceptor(
        dio: dio,
        tokenStorage: tokenStorage,
        refreshTokens: (refreshToken) async {
          refreshCallCount++;
          await Future<void>.delayed(const Duration(milliseconds: 50));
          expect(refreshToken, 'valid-refresh');
          return const AuthTokensDto(
            accessToken: 'new-access',
            refreshToken: 'new-refresh',
            expiresIn: 900,
            user: UserDto(
              id: 1,
              email: 'user@example.com',
              displayName: 'User',
              roles: ['BUYER'],
            ),
          );
        },
        onSessionExpired: () {},
      ),
    );

    final responses = await Future.wait([
      dio.get<Map<String, dynamic>>('/auth/me'),
      dio.get<Map<String, dynamic>>('/categories'),
      dio.get<Map<String, dynamic>>('/protected'),
    ]);

    expect(refreshCallCount, 1);
    expect(responses.every((response) => response.statusCode == 200), isTrue);
    expect(await tokenStorage.readAccessToken(), 'new-access');
    expect(await tokenStorage.readRefreshToken(), 'new-refresh');
  });
}

class _StatusCodeAdapter implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    final authorization = '${options.headers['Authorization']}';
    final statusCode = authorization.contains('expired-access') ? 401 : 200;
    return ResponseBody.fromString(
      '{"ok":true}',
      statusCode,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

class _InMemoryTokenStorage extends TokenStorage {
  _InMemoryTokenStorage({this.accessToken, this.refreshToken});

  String? accessToken;
  String? refreshToken;

  @override
  Future<String?> readAccessToken() async => accessToken;

  @override
  Future<String?> readRefreshToken() async => refreshToken;

  @override
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  @override
  Future<void> clear() async {
    accessToken = null;
    refreshToken = null;
  }
}
