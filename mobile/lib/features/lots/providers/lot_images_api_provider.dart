import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_provider.dart';
import '../data/lot_images_api.dart';

final lotImagesApiProvider = Provider<LotImagesApi>((ref) {
  return LotImagesApi(ref.watch(dioClientProvider));
});
