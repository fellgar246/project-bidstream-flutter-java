import 'package:dio/dio.dart';

import '../../features/auth/data/auth_dto.dart';
import 'token_storage.dart';

typedef RefreshTokensCallback =
    Future<AuthTokensDto> Function(String refreshToken);

class AuthInterceptor extends Interceptor {
  AuthInterceptor({
    required this.dio,
    required this._tokenStorage,
    required this._refreshTokens,
    required this._onSessionExpired,
  });

  static const _retriedExtraKey = 'auth_retried';

  final Dio dio;
  final TokenStorage _tokenStorage;
  final RefreshTokensCallback _refreshTokens;
  final void Function() _onSessionExpired;

  Future<AuthTokensDto>? _refreshFuture;

  static const _authPaths = ['/auth/login', '/auth/register', '/auth/refresh'];

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    if (options.extra[_retriedExtraKey] == true || _isAuthPath(options.path)) {
      handler.next(options);
      return;
    }

    _tokenStorage
        .readAccessToken()
        .then((accessToken) {
          if (accessToken != null && accessToken.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $accessToken';
          }
          handler.next(options);
        })
        .catchError((_) {
          handler.next(options);
        });
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    if (err.requestOptions.extra[_retriedExtraKey] == true) {
      handler.next(err);
      return;
    }

    if (err.response?.statusCode != 401 ||
        _isAuthPath(err.requestOptions.path)) {
      handler.next(err);
      return;
    }

    _recoverFromUnauthorized(err, handler);
  }

  Future<void> _recoverFromUnauthorized(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    try {
      final refreshToken = await _tokenStorage.readRefreshToken();
      if (refreshToken == null || refreshToken.isEmpty) {
        _onSessionExpired();
        handler.next(err);
        return;
      }

      _refreshFuture ??= _refreshTokens(refreshToken).whenComplete(() {
        _refreshFuture = null;
      });
      final tokens = await _refreshFuture!;

      await _tokenStorage.saveTokens(
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      );

      final response = await _retry(err.requestOptions, tokens.accessToken);
      handler.resolve(response);
    } catch (_) {
      _refreshFuture = null;
      _onSessionExpired();
      handler.next(err);
    }
  }

  Future<Response<dynamic>> _retry(
    RequestOptions requestOptions,
    String accessToken,
  ) {
    final headers = Map<String, dynamic>.from(requestOptions.headers);
    headers['Authorization'] = 'Bearer $accessToken';
    return dio.fetch<dynamic>(
      requestOptions.copyWith(
        headers: headers,
        extra: {...requestOptions.extra, _retriedExtraKey: true},
      ),
    );
  }

  bool _isAuthPath(String path) {
    return _authPaths.any((authPath) => path.endsWith(authPath));
  }
}
