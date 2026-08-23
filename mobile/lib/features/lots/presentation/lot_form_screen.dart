import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/locale_provider.dart';
import '../providers/lot_form_provider.dart';

class LotFormScreen extends ConsumerStatefulWidget {
  const LotFormScreen({super.key});

  @override
  ConsumerState<LotFormScreen> createState() => _LotFormScreenState();
}

class _LotFormScreenState extends ConsumerState<LotFormScreen> {
  final _titleController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _startingPriceController = TextEditingController();
  final _minIncrementController = TextEditingController();
  final _reservePriceController = TextEditingController();
  final int _categoryId = 1;

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    _startingPriceController.dispose();
    _minIncrementController.dispose();
    _reservePriceController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final lot = await ref
        .read(lotFormProvider.notifier)
        .submit(
          title: _titleController.text.trim(),
          description: _descriptionController.text.trim(),
          categoryId: _categoryId,
          startingPrice: _startingPriceController.text.trim(),
          minIncrement: _minIncrementController.text.trim(),
          reservePrice: _reservePriceController.text.trim(),
        );
    if (lot != null && mounted) {
      context.go('/seller/lots');
    }
  }

  @override
  Widget build(BuildContext context) {
    final formState = ref.watch(lotFormProvider);
    final l10n = context.l10n;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.lotFormTitle)),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          TextField(
            controller: _titleController,
            decoration: InputDecoration(labelText: l10n.lotTitleLabel),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _descriptionController,
            decoration: InputDecoration(labelText: l10n.lotDescriptionLabel),
            maxLines: 3,
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _startingPriceController,
            decoration: InputDecoration(
              labelText: l10n.lotStartingPriceLabel,
              errorText: formState.fieldErrors['startingPrice'],
            ),
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _minIncrementController,
            decoration: InputDecoration(
              labelText: l10n.lotMinIncrementLabel,
              errorText: formState.fieldErrors['minIncrement'],
            ),
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _reservePriceController,
            decoration: InputDecoration(labelText: l10n.lotReservePriceLabel),
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
          ),
          const SizedBox(height: 24),
          FilledButton(
            onPressed: formState.isSubmitting ? null : _submit,
            child: formState.isSubmitting
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : Text(l10n.lotCreateAction),
          ),
        ],
      ),
    );
  }
}
