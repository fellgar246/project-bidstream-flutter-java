class PlaceBidResponseDto {
  const PlaceBidResponseDto({
    required this.bid,
    required this.lot,
    required this.youAreHighestBidder,
  });

  final BidSummaryDto bid;
  final PlaceBidLotSummaryDto lot;
  final bool youAreHighestBidder;

  factory PlaceBidResponseDto.fromJson(Map<String, dynamic> json) {
    return PlaceBidResponseDto(
      bid: BidSummaryDto.fromJson(json['bid'] as Map<String, dynamic>),
      lot: PlaceBidLotSummaryDto.fromJson(json['lot'] as Map<String, dynamic>),
      youAreHighestBidder: json['youAreHighestBidder'] as bool? ?? false,
    );
  }
}

class BidSummaryDto {
  const BidSummaryDto({
    required this.id,
    required this.amount,
    required this.placedAt,
    required this.bidderDisplayName,
  });

  final int id;
  final String amount;
  final String placedAt;
  final String bidderDisplayName;

  factory BidSummaryDto.fromJson(Map<String, dynamic> json) {
    return BidSummaryDto(
      id: json['id'] as int,
      amount: json['amount'] as String,
      placedAt: json['placedAt'] as String,
      bidderDisplayName: json['bidderDisplayName'] as String,
    );
  }
}

class PlaceBidLotSummaryDto {
  const PlaceBidLotSummaryDto({
    required this.currentPrice,
    required this.bidCount,
    this.scheduledEndAt,
    required this.extended,
  });

  final String currentPrice;
  final int bidCount;
  final String? scheduledEndAt;
  final bool extended;

  factory PlaceBidLotSummaryDto.fromJson(Map<String, dynamic> json) {
    return PlaceBidLotSummaryDto(
      currentPrice: json['currentPrice'] as String,
      bidCount: json['bidCount'] as int,
      scheduledEndAt: json['scheduledEndAt'] as String?,
      extended: json['extended'] as bool? ?? false,
    );
  }
}
