import 'package:dio/dio.dart';

import '../../../core/network/api_exception.dart';
import 'auth_dto.dart';

class AuthApi {
  AuthApi(this._dio);

  final Dio _dio;

  Future<UserDto> register({
    required String email,
    required String password,
    required String displayName,
  }) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/auth/register',
        data: {
          'email': email,
          'password': password,
          'displayName': displayName,
        },
      );
      return UserDto.fromJson(response.data!);
    } on DioException catch (error) {
      throw _unwrap(error);
    }
  }

  Future<AuthTokensDto> login({
    required String email,
    required String password,
  }) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/auth/login',
        data: {'email': email, 'password': password},
      );
      return AuthTokensDto.fromJson(response.data!);
    } on DioException catch (error) {
      throw _unwrap(error);
    }
  }

  Future<AuthTokensDto> refresh({required String refreshToken}) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/auth/refresh',
        data: {'refreshToken': refreshToken},
      );
      return AuthTokensDto.fromJson(response.data!);
    } on DioException catch (error) {
      throw _unwrap(error);
    }
  }

  Future<void> logout({required String refreshToken}) async {
    try {
      await _dio.post<void>(
        '/auth/logout',
        data: {'refreshToken': refreshToken},
      );
    } on DioException catch (error) {
      throw _unwrap(error);
    }
  }

  Future<UserDto> me() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>('/auth/me');
      return UserDto.fromJson(response.data!);
    } on DioException catch (error) {
      throw _unwrap(error);
    }
  }

  Future<AuthTokensDto> applySeller() async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/me/seller-application',
      );
      return AuthTokensDto.fromJson(response.data!);
    } on DioException catch (error) {
      throw _unwrap(error);
    }
  }

  Exception _unwrap(DioException error) {
    if (error.error is ApiException) {
      return error.error! as ApiException;
    }
    return error;
  }
}
