class LotFilters {
  const LotFilters({
    this.status = 'LIVE',
    this.categoryId,
    this.minPriceCents,
    this.maxPriceCents,
    this.sellerId,
    this.query,
    this.sort = 'endingSoon',
    this.size = 20,
  });

  final String status;
  final int? categoryId;
  final int? minPriceCents;
  final int? maxPriceCents;
  final int? sellerId;
  final String? query;
  final String sort;
  final int size;

  LotFilters copyWith({
    String? status,
    int? categoryId,
    int? minPriceCents,
    int? maxPriceCents,
    int? sellerId,
    String? query,
    String? sort,
    int? size,
  }) {
    return LotFilters(
      status: status ?? this.status,
      categoryId: categoryId ?? this.categoryId,
      minPriceCents: minPriceCents ?? this.minPriceCents,
      maxPriceCents: maxPriceCents ?? this.maxPriceCents,
      sellerId: sellerId ?? this.sellerId,
      query: query ?? this.query,
      sort: sort ?? this.sort,
      size: size ?? this.size,
    );
  }

  Map<String, dynamic> toQueryParams(int page, {bool facets = false}) {
    return {
      'status': status,
      if (categoryId != null) 'categoryId': categoryId,
      if (minPriceCents != null) 'minPriceCents': minPriceCents,
      if (maxPriceCents != null) 'maxPriceCents': maxPriceCents,
      if (sellerId != null) 'sellerId': sellerId,
      if (query != null && query!.isNotEmpty) 'q': query,
      if (facets) 'facets': true,
      'page': page,
      'size': size,
      'sort': sort,
    };
  }

  LotFilters clearFilters() {
    return LotFilters(status: status, sort: sort, size: size);
  }

  @override
  bool operator ==(Object other) {
    return other is LotFilters &&
        other.status == status &&
        other.categoryId == categoryId &&
        other.minPriceCents == minPriceCents &&
        other.maxPriceCents == maxPriceCents &&
        other.sellerId == sellerId &&
        other.query == query &&
        other.sort == sort &&
        other.size == size;
  }

  @override
  int get hashCode => Object.hash(
    status,
    categoryId,
    minPriceCents,
    maxPriceCents,
    sellerId,
    query,
    sort,
    size,
  );
}
