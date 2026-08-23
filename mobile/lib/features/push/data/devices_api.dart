import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_provider.dart';

final devicesApiProvider = Provider<DevicesApi>((ref) {
  return DevicesApi(ref.watch(dioClientProvider).dio);
});

class DevicesApi {
  DevicesApi(this._dio);

  final Dio _dio;

  Future<void> register({
    required String token,
    required String platform,
  }) async {
    await _dio.post<void>(
      '/me/devices',
      data: {'token': token, 'platform': platform},
    );
  }

  Future<void> unregister(String token) async {
    await _dio.delete<void>('/me/devices/$token');
  }
}
