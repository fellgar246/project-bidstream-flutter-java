import 'dart:typed_data';

import 'package:bidstream/core/network/dio_client.dart';
import 'package:bidstream/features/lots/data/lot_image_dto.dart';
import 'package:bidstream/features/lots/data/lot_images_api.dart';
import 'package:bidstream/features/lots/providers/image_upload_provider.dart';
import 'package:bidstream/features/lots/providers/image_upload_state.dart';
import 'package:bidstream/features/lots/providers/lot_images_api_provider.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('ca0411_retryReusesSamePresignedRow', () async {
    final api = _FakeLotImagesApi();
    final container = ProviderContainer(
      overrides: [lotImagesApiProvider.overrideWithValue(api)],
    );
    addTearDown(container.dispose);

    final notifier = container.read(imageUploadProvider(1).notifier);
    notifier.addSelected(localId: 'local-1', bytes: Uint8List.fromList([1, 2, 3]));

    await _pumpUntil(
      container,
      1,
      (items) => items.any((item) => item.phase == ImageUploadPhase.error),
    );

    api.failUpload = false;
    await notifier.retry('local-1');

    await _pumpUntil(
      container,
      1,
      (items) => items.any((item) => item.phase == ImageUploadPhase.ready),
    );

    expect(api.presignCalls, 1);
    expect(api.confirmCalls, 1);
  });
}

Future<void> _pumpUntil(
  ProviderContainer container,
  int lotId,
  bool Function(List<ImageUploadItem>) predicate,
) async {
  for (var i = 0; i < 50; i++) {
    await Future<void>.delayed(Duration.zero);
    final items = container.read(imageUploadProvider(lotId));
    if (predicate(items)) {
      return;
    }
  }
  fail('Timed out waiting for upload state');
}

class _FakeLotImagesApi extends LotImagesApi {
  _FakeLotImagesApi() : super(DioClient());

  int presignCalls = 0;
  int confirmCalls = 0;
  bool failUpload = true;

  @override
  Future<PresignImageDto> presign({
    required int lotId,
    required String fileName,
    required String contentType,
    required int sizeBytes,
  }) async {
    presignCalls++;
    return PresignImageDto(
      imageId: 42,
      uploadUrl: 'https://minio.test/upload',
      storageKey: 'lots/$lotId/file.jpg',
      expiresIn: 300,
    );
  }

  @override
  Future<void> uploadBytes({
    required String uploadUrl,
    required Uint8List bytes,
    required String contentType,
    void Function(int sent, int total)? onSendProgress,
  }) async {
    if (failUpload) {
      throw Exception('network cut');
    }
    onSendProgress?.call(bytes.length, bytes.length);
  }

  @override
  Future<LotImageDto> confirm({
    required int lotId,
    required int imageId,
  }) async {
    confirmCalls++;
    return const LotImageDto(
      id: 42,
      url: 'https://cdn.test/42.jpg',
      thumbnailUrl: null,
      position: 0,
      contentType: 'image/jpeg',
      status: 'READY',
    );
  }
}
