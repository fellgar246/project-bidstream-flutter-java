import 'dart:math' as math;

import 'package:flutter/material.dart';

class CountdownArc extends StatelessWidget {
  const CountdownArc({
    super.key,
    required this.remaining,
    required this.total,
    required this.label,
  });

  final Duration remaining;
  final Duration total;
  final String label;

  @override
  Widget build(BuildContext context) {
    final progress = total.inSeconds == 0 ? 0.0 : remaining.inSeconds / total.inSeconds;
    final urgent = remaining.inSeconds <= 30;
    return SizedBox(
      width: 120,
      height: 120,
      child: Stack(
        alignment: Alignment.center,
        children: [
          CustomPaint(
            size: const Size(120, 120),
            painter: _ArcPainter(progress: progress.clamp(0.0, 1.0), urgent: urgent),
          ),
          Text(label, textAlign: TextAlign.center, style: Theme.of(context).textTheme.titleMedium),
        ],
      ),
    );
  }
}

class _ArcPainter extends CustomPainter {
  _ArcPainter({required this.progress, required this.urgent});

  final double progress;
  final bool urgent;

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width / 2 - 8;
    final background = Paint()
      ..color = Colors.grey.shade300
      ..style = PaintingStyle.stroke
      ..strokeWidth = 8;
    final foreground = Paint()
      ..color = urgent ? Colors.red : Colors.green
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeWidth = 8;

    canvas.drawCircle(center, radius, background);
    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius),
      -math.pi / 2,
      2 * math.pi * progress,
      false,
      foreground,
    );
  }

  @override
  bool shouldRepaint(covariant _ArcPainter oldDelegate) {
    return oldDelegate.progress != progress || oldDelegate.urgent != urgent;
  }
}
