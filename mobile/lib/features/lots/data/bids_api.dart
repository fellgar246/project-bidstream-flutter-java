import 'package:dio/dio.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import 'bid_dto.dart';

class BidsApi {
  BidsApi(this._client);

  final DioClient _client;

  Future<PlaceBidResponseDto> placeBid(
    int lotId,
    String amount,
    String clientRequestId,
  ) async {
    try {
      final response = await _client.dio.post<Map<String, dynamic>>(
        '/lots/$lotId/bids',
        data: {'amount': amount, 'clientRequestId': clientRequestId},
      );
      return PlaceBidResponseDto.fromJson(response.data ?? {});
    } on DioException catch (error) {
      throw _mapError(error);
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
