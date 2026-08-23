import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import '../data/bid_history_compute.dart';

class BidHistoryChart extends StatelessWidget {
  const BidHistoryChart({super.key, required this.amounts});

  final List<String> amounts;

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<BidHistoryPoint>>(
      future: amounts.length > 200
          ? compute(computeBidHistorySeries, amounts)
          : Future.value(computeBidHistorySeries(amounts)),
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const SizedBox(
            height: 120,
            child: Center(child: CircularProgressIndicator()),
          );
        }
        final points = snapshot.data!;
        if (points.isEmpty) {
          return const SizedBox.shrink();
        }
        final maxAmount = points
            .map((p) => p.amount)
            .reduce((a, b) => a > b ? a : b);
        return SizedBox(
          height: 120,
          child: CustomPaint(
            painter: _BidChartPainter(points: points, maxAmount: maxAmount),
            child: Container(),
          ),
        );
      },
    );
  }
}

class _BidChartPainter extends CustomPainter {
  _BidChartPainter({required this.points, required this.maxAmount});

  final List<BidHistoryPoint> points;
  final double maxAmount;

  @override
  void paint(Canvas canvas, Size size) {
    if (points.length < 2 || maxAmount <= 0) {
      return;
    }
    final paint = Paint()
      ..color = Colors.blue
      ..strokeWidth = 2
      ..style = PaintingStyle.stroke;
    final path = Path();
    for (var i = 0; i < points.length; i++) {
      final x = (i / (points.length - 1)) * size.width;
      final y = size.height - (points[i].amount / maxAmount) * size.height;
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
    }
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant _BidChartPainter oldDelegate) => true;
}
