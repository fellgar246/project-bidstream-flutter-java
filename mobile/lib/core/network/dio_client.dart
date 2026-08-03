import 'package:dio/dio.dart';

import '../config/app_config.dart';
import 'api_exception.dart';

class DioClient {
  DioClient({Dio? dio, String? baseUrl})
      : _dio = dio ??
            Dio(
              BaseOptions(
                baseUrl: baseUrl ?? AppConfig.apiBaseUrl,
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

  final Dio _dio;

  Dio get dio => _dio;
}
