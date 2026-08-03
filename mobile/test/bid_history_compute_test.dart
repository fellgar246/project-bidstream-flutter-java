import 'package:flutter_test/flutter_test.dart';

import 'package:bidstream/features/lots/data/bid_history_compute.dart';

void main() {
  test('computeBidHistorySeries builds points from amount strings', () {
    final amounts = List.generate(250, (i) => '${100 + i}.00');
    final series = computeBidHistorySeries(amounts);
    expect(series, hasLength(250));
    expect(series.first.amount, 100);
    expect(series.last.amount, 349);
  });
}
