import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/network/dio_provider.dart';
import '../../../core/realtime/stomp_client.dart';

class LotEventsApi {
  LotEventsApi(this._dio);

  final DioClient _dio;

  Future<List<LotEventMessage>> fetchAfter(int lotId, int afterEventId) async {
    final response = await _dio.dio.get<List<dynamic>>(
      '/lots/$lotId/events',
      queryParameters: {
        'afterEventId': afterEventId,
        'limit': 100,
      },
    );
    final data = response.data ?? [];
    return data
        .map((item) => LotEventMessage.fromJson(Map<String, dynamic>.from(item as Map)))
        .toList();
  }
}

final lotEventsApiProvider = Provider<LotEventsApi>((ref) {
  return LotEventsApi(ref.watch(dioClientProvider));
});
