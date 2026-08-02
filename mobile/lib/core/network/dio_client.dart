import 'package:dio/dio.dart';

import 'api_exception.dart';

class DioClient {
  DioClient({Dio? dio, String? baseUrl})
      : _dio = dio ??
            Dio(
              BaseOptions(
                baseUrl: baseUrl ?? _defaultBaseUrl,
                connectTimeout: const Duration(seconds: 10),
                receiveTimeout: const Duration(seconds: 10),
                headers: const {'Accept': 'application/json'},
              ),
            ) {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onError: (error, handler) {
          final data = error.response?.data;
          if (data is Map<String, dynamic> && data.containsKey('error')) {
            handler.reject(
              DioException(
                requestOptions: error.requestOptions,
                response: error.response,
                type: error.type,
                error: ApiException.fromJson(data),
              ),
            );
            return;
          }
          handler.next(error);
        },
      ),
    );
  }

  static const _defaultBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080/api/v1',
  );

  final Dio _dio;

  Dio get dio => _dio;
}
