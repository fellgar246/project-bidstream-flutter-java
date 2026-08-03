import 'package:dio/dio.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import 'lot_dto.dart';
import 'lot_filters.dart';

class LotsApi {
  LotsApi(this._client);

  final DioClient _client;

  Future<LotPageDto> fetchLots(LotFilters filters, int page, {bool facets = false}) async {
    try {
      final response = await _client.dio.get<Map<String, dynamic>>(
        '/lots',
        queryParameters: filters.toQueryParams(page, facets: facets),
      );
      return LotPageDto.fromJson(response.data ?? {});
    } on DioException catch (error) {
      throw _mapError(error);
    }
  }

  Future<LotDto> fetchLot(int id) async {
    try {
      final response = await _client.dio.get<Map<String, dynamic>>('/lots/$id');
      return LotDto.fromJson(response.data ?? {});
    } on DioException catch (error) {
      throw _mapError(error);
    }
  }

  Future<List<LotDto>> fetchMyLots() async {
    try {
      final response = await _client.dio.get<List<dynamic>>('/me/lots');
      return (response.data ?? [])
          .map((item) => LotDto.fromJson(item as Map<String, dynamic>))
          .toList();
    } on DioException catch (error) {
      throw _mapError(error);
    }
  }

  Future<LotDto> createLot(Map<String, dynamic> body) async {
    try {
      final response = await _client.dio.post<Map<String, dynamic>>('/lots', data: body);
      return LotDto.fromJson(response.data ?? {});
    } on DioException catch (error) {
      throw _mapError(error);
    }
  }

  Future<LotDto> updateLot(int id, Map<String, dynamic> body) async {
    try {
      final response =
          await _client.dio.patch<Map<String, dynamic>>('/lots/$id', data: body);
      return LotDto.fromJson(response.data ?? {});
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
