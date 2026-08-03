class LotImageDto {
  const LotImageDto({
    required this.id,
    required this.url,
    this.thumbnailUrl,
    required this.position,
    required this.contentType,
    required this.status,
  });

  final int id;
  final String? url;
  final String? thumbnailUrl;
  final int position;
  final String contentType;
  final String status;

  factory LotImageDto.fromJson(Map<String, dynamic> json) {
    return LotImageDto(
      id: json['id'] as int,
      url: json['url'] as String?,
      thumbnailUrl: json['thumbnailUrl'] as String?,
      position: json['position'] as int,
      contentType: json['contentType'] as String,
      status: json['status'] as String,
    );
  }
}

class PresignImageDto {
  const PresignImageDto({
    required this.imageId,
    required this.uploadUrl,
    required this.storageKey,
    required this.expiresIn,
  });

  final int imageId;
  final String uploadUrl;
  final String storageKey;
  final int expiresIn;

  factory PresignImageDto.fromJson(Map<String, dynamic> json) {
    return PresignImageDto(
      imageId: json['imageId'] as int,
      uploadUrl: json['uploadUrl'] as String,
      storageKey: json['storageKey'] as String,
      expiresIn: json['expiresIn'] as int,
    );
  }
}
