import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:bidstream/core/push/push_service.dart';

void main() {
  test('simulated push invokes tap handler with deep link', () async {
    String? tapped;
    final container = ProviderContainer();
    addTearDown(container.dispose);

    final service = container.read(pushServiceProvider);
    await service.initialize(onTap: (link) => tapped = link);
    service.handleSimulatedMessage({'deepLink': 'bidstream://lots/99'});

    expect(tapped, 'bidstream://lots/99');
  });
}
