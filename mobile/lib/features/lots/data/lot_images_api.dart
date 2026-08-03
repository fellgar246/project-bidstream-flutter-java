import 'dart:typed_data';

import 'package:dio/dio.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import 'lot_image_dto.dart';

class LotImagesApi {
  LotImagesApi(this._client);

  final DioClient _client;

  Future<PresignImageDto> presign({
    required int lotId,
    required String fileName,
    required String contentType,
    required int sizeBytes,
  }) async {
    try {
      final response = await _client.dio.post<Map<String, dynamic>>(
        '/lots/$lotId/images/presign',
        data: {
          'fileName': fileName,
          'contentType': contentType,
          'sizeBytes': sizeBytes,
        },
      );
      return PresignImageDto.fromJson(response.data ?? {});
    } on DioException catch (error) {
      throw _mapError(error);
    }
  }

  Future<LotImageDto> confirm({
    required int lotId,
    required int imageId,
  }) async {
    try {
      final response = await _client.dio.post<Map<String, dynamic>>(
        '/lots/$lotId/images/$imageId/confirm',
      );
      final image = response.data?['image'] as Map<String, dynamic>? ?? {};
      return LotImageDto.fromJson(image);
    } on DioException catch (error) {
      throw _mapError(error);
    }
  }

  Future<void> deleteImage({
    required int lotId,
    required int imageId,
  }) async {
    try {
      await _client.dio.delete<void>('/lots/$lotId/images/$imageId');
    } on DioException catch (error) {
      throw _mapError(error);
    }
  }

  Future<List<LotImageDto>> reorder({
    required int lotId,
    required List<int> order,
  }) async {
    try {
      final response = await _client.dio.patch<List<dynamic>>(
        '/lots/$lotId/images/order',
        data: {'order': order},
      );
      return (response.data ?? [])
          .map((item) => LotImageDto.fromJson(item as Map<String, dynamic>))
          .toList();
    } on DioException catch (error) {
      throw _mapError(error);
    }
  }

  Future<void> uploadBytes({
    required String uploadUrl,
    required Uint8List bytes,
    required String contentType,
    void Function(int sent, int total)? onSendProgress,
  }) async {
    final uploadClient = Dio();
    try {
      await uploadClient.put<void>(
        uploadUrl,
        data: Stream.fromIterable([bytes]),
        options: Options(
          headers: {
            'Content-Type': contentType,
            'Content-Length': bytes.length.toString(),
          },
        ),
        onSendProgress: onSendProgress,
      );
    } on DioException catch (error) {
      throw ApiException(
        code: 'upload_failed',
        message: error.message ?? 'Direct upload failed',
        details: const {},
      );
    }
  }

  ApiException _mapError(DioException error) {
    if (error.error is ApiException) {
      return error.error as ApiException;
    }
    return ApiException(
      code: 'network_error',
      message: error.message ?? 'Network request failed',
      details: const {},
    );
  }
}
