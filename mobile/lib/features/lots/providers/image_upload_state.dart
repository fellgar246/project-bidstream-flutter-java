import 'dart:typed_data';

enum ImageUploadPhase { selected, uploading, confirming, ready, error }

class ImageUploadItem {
  const ImageUploadItem({
    required this.localId,
    this.imageId,
    this.uploadUrl,
    this.fileName = 'photo.jpg',
    this.contentType = 'image/jpeg',
    required this.bytes,
    this.phase = ImageUploadPhase.selected,
    this.progress = 0,
    this.errorMessage,
    this.readyImageId,
  });

  final String localId;
  final int? imageId;
  final String? uploadUrl;
  final String fileName;
  final String contentType;
  final Uint8List bytes;
  final ImageUploadPhase phase;
  final double progress;
  final String? errorMessage;
  final int? readyImageId;

  ImageUploadItem copyWith({
    int? imageId,
    String? uploadUrl,
    ImageUploadPhase? phase,
    double? progress,
    String? errorMessage,
    int? readyImageId,
    bool clearError = false,
  }) {
    return ImageUploadItem(
      localId: localId,
      imageId: imageId ?? this.imageId,
      uploadUrl: uploadUrl ?? this.uploadUrl,
      fileName: fileName,
      contentType: contentType,
      bytes: bytes,
      phase: phase ?? this.phase,
      progress: progress ?? this.progress,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      readyImageId: readyImageId ?? this.readyImageId,
    );
  }
}
