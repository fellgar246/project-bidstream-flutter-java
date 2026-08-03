import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/bids_api.dart';
import '../../../core/network/dio_provider.dart';

final bidsApiProvider = Provider<BidsApi>((ref) {
  return BidsApi(ref.watch(dioClientProvider));
});
