import 'package:dio/dio.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import 'category_dto.dart';

class CategoriesApi {
  CategoriesApi(this._client);

  final DioClient _client;

  Future<List<CategoryDto>> fetchCategories() async {
    try {
      final response = await _client.dio.get<List<dynamic>>('/categories');
      final data = response.data ?? [];
      return data
          .map((item) => CategoryDto.fromJson(item as Map<String, dynamic>))
          .toList();
    } on DioException catch (error) {
      if (error.error is ApiException) {
        throw error.error as ApiException;
      }
      throw ApiException(
        code: 'network_error',
        message: error.message ?? 'Network request failed',
        details: const {},
      );
    }
  }
}
