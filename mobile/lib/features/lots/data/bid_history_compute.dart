import 'package:decimal/decimal.dart';

class BidHistoryPoint {
  const BidHistoryPoint({required this.index, required this.amount});

  final int index;
  final double amount;
}

List<BidHistoryPoint> buildBidHistorySeries(List<Decimal> amounts) {
  return List.generate(amounts.length, (index) {
    return BidHistoryPoint(index: index, amount: amounts[index].toDouble());
  });
}

List<BidHistoryPoint> computeBidHistorySeries(List<String> amountStrings) {
  final amounts = amountStrings.map(Decimal.parse).toList();
  return buildBidHistorySeries(amounts);
}
