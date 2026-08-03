import 'package:decimal/decimal.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/lot_dto.dart';
import 'lots_list_provider.dart';

class LotFormState {
  const LotFormState({
    this.isSubmitting = false,
    this.fieldErrors = const {},
    this.submittedLot,
  });

  final bool isSubmitting;
  final Map<String, String> fieldErrors;
  final LotDto? submittedLot;

  LotFormState copyWith({
    bool? isSubmitting,
    Map<String, String>? fieldErrors,
    LotDto? submittedLot,
  }) {
    return LotFormState(
      isSubmitting: isSubmitting ?? this.isSubmitting,
      fieldErrors: fieldErrors ?? this.fieldErrors,
      submittedLot: submittedLot ?? this.submittedLot,
    );
  }
}

final lotFormProvider = NotifierProvider<LotFormNotifier, LotFormState>(LotFormNotifier.new);

class LotFormNotifier extends Notifier<LotFormState> {
  @override
  LotFormState build() => const LotFormState();

  Future<LotDto?> submit({
    required String title,
    required String description,
    required int categoryId,
    required String startingPrice,
    required String minIncrement,
    String? reservePrice,
  }) async {
    final clientErrors = _validateClient(
      startingPrice: startingPrice,
      minIncrement: minIncrement,
    );
    if (clientErrors.isNotEmpty) {
      state = state.copyWith(fieldErrors: clientErrors);
      return null;
    }

    state = state.copyWith(isSubmitting: true, fieldErrors: {});
    try {
      final lot = await ref.read(lotsApiProvider).createLot({
        'title': title,
        'description': description,
        'categoryId': categoryId,
        'startingPrice': startingPrice,
        'minIncrement': minIncrement,
        if (reservePrice != null && reservePrice.isNotEmpty)
          'reservePrice': reservePrice,
      });
      state = state.copyWith(isSubmitting: false, submittedLot: lot);
      return lot;
    } on Exception catch (error) {
      final fieldErrors = _mapApiErrors(error);
      state = state.copyWith(isSubmitting: false, fieldErrors: fieldErrors);
      return null;
    }
  }

  Map<String, String> _validateClient({
    required String startingPrice,
    required String minIncrement,
  }) {
    final errors = <String, String>{};
    try {
      final start = Decimal.parse(startingPrice);
      final increment = Decimal.parse(minIncrement);
      if (start <= Decimal.zero) {
        errors['startingPrice'] = 'Must be greater than zero';
      }
      if (increment <= Decimal.zero) {
        errors['minIncrement'] = 'Must be greater than zero';
      }
    } on FormatException {
      errors['startingPrice'] = 'Invalid amount';
    }
    return errors;
  }

  Map<String, String> _mapApiErrors(Object error) {
    if (error is! Exception) {
      return {'form': 'Could not save lot'};
    }
    // ApiException details are attached by interceptor in Dio layer.
    return {'form': error.toString()};
  }
}
