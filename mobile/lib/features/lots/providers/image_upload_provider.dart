import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/lot_images_api.dart';
import 'image_upload_state.dart';
import 'lot_images_api_provider.dart';

final imageUploadProvider =
    NotifierProvider.family<ImageUploadNotifier, List<ImageUploadItem>, int>(
  ImageUploadNotifier.new,
);

class ImageUploadNotifier extends FamilyNotifier<List<ImageUploadItem>, int> {
  @override
  List<ImageUploadItem> build(int lotId) => const [];

  LotImagesApi get _api => ref.read(lotImagesApiProvider);

  void addSelected({
    required String localId,
    required Uint8List bytes,
    String fileName = 'photo.jpg',
    String contentType = 'image/jpeg',
  }) {
    state = [
      ...state,
      ImageUploadItem(
        localId: localId,
        fileName: fileName,
        contentType: contentType,
        bytes: bytes,
      ),
    ];
    _startUpload(localId, reuseExistingPresign: false);
  }

  Future<void> retry(String localId) async {
    final item = _find(localId);
    if (item == null) {
      return;
    }
    _update(
      localId,
      item.copyWith(phase: ImageUploadPhase.selected, clearError: true, progress: 0),
    );
    await _startUpload(localId, reuseExistingPresign: item.imageId != null);
  }

  Future<void> _startUpload(String localId, {required bool reuseExistingPresign}) async {
    final item = _find(localId);
    if (item == null) {
      return;
    }

    try {
      final presign = await _resolvePresign(item, reuseExistingPresign);

      _update(
        localId,
        item.copyWith(
          imageId: presign.imageId,
          uploadUrl: presign.uploadUrl,
          phase: ImageUploadPhase.uploading,
          progress: 0,
          clearError: true,
        ),
      );

      await _api.uploadBytes(
        uploadUrl: presign.uploadUrl,
        bytes: item.bytes,
        contentType: item.contentType,
        onSendProgress: (sent, total) {
          final current = _find(localId);
          if (current == null) {
            return;
          }
          final progress = total == 0 ? 0.0 : sent / total;
          _update(localId, current.copyWith(progress: progress));
        },
      );

      final uploading = _find(localId);
      if (uploading == null) {
        return;
      }
      _update(localId, uploading.copyWith(phase: ImageUploadPhase.confirming, progress: 1));

      final confirmed = await _api.confirm(lotId: arg, imageId: presign.imageId);
      final confirming = _find(localId);
      if (confirming == null) {
        return;
      }
      _update(
        localId,
        confirming.copyWith(
          phase: ImageUploadPhase.ready,
          readyImageId: confirmed.id,
          progress: 1,
          clearError: true,
        ),
      );
    } catch (error) {
      final failed = _find(localId);
      if (failed == null) {
        return;
      }
      _update(
        localId,
        failed.copyWith(
          phase: ImageUploadPhase.error,
          errorMessage: error.toString(),
        ),
      );
    }
  }

  Future<({int imageId, String uploadUrl})> _resolvePresign(
    ImageUploadItem item,
    bool reuseExistingPresign,
  ) async {
    if (reuseExistingPresign && item.imageId != null && item.uploadUrl != null) {
      return (imageId: item.imageId!, uploadUrl: item.uploadUrl!);
    }
    final presign = await _api.presign(
      lotId: arg,
      fileName: item.fileName,
      contentType: item.contentType,
      sizeBytes: item.bytes.length,
    );
    return (imageId: presign.imageId, uploadUrl: presign.uploadUrl);
  }

  void removeLocal(String localId) {
    state = state.where((item) => item.localId != localId).toList();
  }

  ImageUploadItem? _find(String localId) {
    for (final item in state) {
      if (item.localId == localId) {
        return item;
      }
    }
    return null;
  }

  void _update(String localId, ImageUploadItem updated) {
    state = [
      for (final item in state)
        if (item.localId == localId) updated else item,
    ];
  }
}
