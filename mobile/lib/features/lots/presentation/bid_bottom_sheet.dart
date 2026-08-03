import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/app_strings.dart';
import '../data/lot_dto.dart';
import '../providers/bid_provider.dart';

class BidBottomSheet extends ConsumerStatefulWidget {
  const BidBottomSheet({super.key, required this.lot});

  final LotDto lot;

  @override
  ConsumerState<BidBottomSheet> createState() => _BidBottomSheetState();
}

class _BidBottomSheetState extends ConsumerState<BidBottomSheet> {
  late final TextEditingController _amountController;

  @override
  void initState() {
    super.initState();
    _amountController = TextEditingController(text: _suggestedAmount(widget.lot, 1));
  }

  @override
  void dispose() {
    _amountController.dispose();
    super.dispose();
  }

  String _suggestedAmount(LotDto lot, int multiplier) {
    final increment = Decimal.parse(lot.minIncrement);
    if (lot.bidCount == 0) {
      return (Decimal.parse(lot.startingPrice) + increment * Decimal.fromInt(multiplier))
          .toStringAsFixed(2);
    }
    return (Decimal.parse(lot.currentPrice) + increment * Decimal.fromInt(multiplier))
        .toStringAsFixed(2);
  }

  Future<void> _submit() async {
    final amount = _amountController.text.trim();
    if (!_isValidDecimal(amount)) {
      ref.read(bidProvider(widget.lot.id).notifier).setValidationError(AppStrings.bidInvalidAmount);
      return;
    }
    final success = await ref.read(bidProvider(widget.lot.id).notifier).submitBid(amount);
    if (success && mounted) {
      Navigator.of(context).pop(true);
    }
  }

  bool _isValidDecimal(String value) {
    try {
      Decimal.parse(value);
      return true;
    } catch (_) {
      return false;
    }
  }

  @override
  Widget build(BuildContext context) {
    final bidState = ref.watch(bidProvider(widget.lot.id));
    final lot = widget.lot;

    return Padding(
      padding: EdgeInsets.only(
        left: 24,
        right: 24,
        top: 24,
        bottom: MediaQuery.of(context).viewInsets.bottom + 24,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(AppStrings.bidTitle, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          Text('${AppStrings.lotCurrentPrice}: ${lot.currentPrice}'),
          Text('${AppStrings.bidMinIncrement}: ${lot.minIncrement}'),
          const SizedBox(height: 16),
          Wrap(
            spacing: 8,
            children: [
              for (final multiplier in [1, 2, 5])
                ActionChip(
                  label: Text('+${multiplier}x'),
                  onPressed: bidState.isSubmitting
                      ? null
                      : () => _amountController.text = _suggestedAmount(lot, multiplier),
                ),
            ],
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _amountController,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: InputDecoration(
              labelText: AppStrings.bidAmountLabel,
              border: const OutlineInputBorder(),
            ),
            enabled: !bidState.isSubmitting,
          ),
          if (bidState.errorMessage != null) ...[
            const SizedBox(height: 8),
            Text(
              bidState.errorMessage!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
          const SizedBox(height: 16),
          FilledButton(
            onPressed: bidState.isSubmitting ? null : _submit,
            child: bidState.isSubmitting
                ? const SizedBox(
                    height: 20,
                    width: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text(AppStrings.bidPlaceAction),
          ),
        ],
      ),
    );
  }
}
