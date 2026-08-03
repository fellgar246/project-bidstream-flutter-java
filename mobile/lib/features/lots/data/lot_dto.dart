class LotDto {
  const LotDto({
    required this.id,
    required this.title,
    required this.description,
    required this.category,
    required this.seller,
    required this.startingPrice,
    required this.minIncrement,
    required this.currentPrice,
    required this.bidCount,
    required this.hasReserve,
    required this.reserveMet,
    required this.status,
    this.scheduledStartAt,
    this.scheduledEndAt,
    this.actualEndAt,
    required this.watched,
    required this.canEdit,
    required this.canBid,
  });

  final int id;
  final String title;
  final String description;
  final LotCategoryDto category;
  final LotSellerDto seller;
  final String startingPrice;
  final String minIncrement;
  final String currentPrice;
  final int bidCount;
  final bool hasReserve;
  final bool reserveMet;
  final String status;
  final String? scheduledStartAt;
  final String? scheduledEndAt;
  final String? actualEndAt;
  final bool watched;
  final bool canEdit;
  final bool canBid;

  factory LotDto.fromJson(Map<String, dynamic> json) {
    return LotDto(
      id: json['id'] as int,
      title: json['title'] as String,
      description: json['description'] as String,
      category: LotCategoryDto.fromJson(json['category'] as Map<String, dynamic>),
      seller: LotSellerDto.fromJson(json['seller'] as Map<String, dynamic>),
      startingPrice: json['startingPrice'] as String,
      minIncrement: json['minIncrement'] as String,
      currentPrice: json['currentPrice'] as String,
      bidCount: json['bidCount'] as int,
      hasReserve: json['hasReserve'] as bool,
      reserveMet: json['reserveMet'] as bool,
      status: json['status'] as String,
      scheduledStartAt: json['scheduledStartAt'] as String?,
      scheduledEndAt: json['scheduledEndAt'] as String?,
      actualEndAt: json['actualEndAt'] as String?,
      watched: json['watched'] as bool? ?? false,
      canEdit: json['canEdit'] as bool? ?? false,
      canBid: json['canBid'] as bool? ?? false,
    );
  }
}

class LotCategoryDto {
  const LotCategoryDto({required this.id, required this.name});

  final int id;
  final String name;

  factory LotCategoryDto.fromJson(Map<String, dynamic> json) {
    return LotCategoryDto(
      id: json['id'] as int,
      name: json['name'] as String,
    );
  }
}

class LotSellerDto {
  const LotSellerDto({required this.id, required this.displayName});

  final int id;
  final String displayName;

  factory LotSellerDto.fromJson(Map<String, dynamic> json) {
    return LotSellerDto(
      id: json['id'] as int,
      displayName: json['displayName'] as String,
    );
  }
}

class LotPageDto {
  const LotPageDto({required this.content, required this.page});

  final List<LotDto> content;
  final LotPageMetadata page;

  factory LotPageDto.fromJson(Map<String, dynamic> json) {
    final items = (json['content'] as List<dynamic>? ?? [])
        .map((item) => LotDto.fromJson(item as Map<String, dynamic>))
        .toList();
    return LotPageDto(
      content: items,
      page: LotPageMetadata.fromJson(json['page'] as Map<String, dynamic>),
    );
  }
}

class LotPageMetadata {
  const LotPageMetadata({
    required this.number,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  final int number;
  final int size;
  final int totalElements;
  final int totalPages;

  factory LotPageMetadata.fromJson(Map<String, dynamic> json) {
    return LotPageMetadata(
      number: json['number'] as int,
      size: json['size'] as int,
      totalElements: json['totalElements'] as int,
      totalPages: json['totalPages'] as int,
    );
  }
}
