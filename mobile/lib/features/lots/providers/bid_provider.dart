import 'dart:math';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../data/bid_dto.dart';
import 'bids_api_provider.dart';
import 'lot_detail_provider.dart';

final bidProvider = NotifierProvider.family<BidNotifier, BidState, int>(BidNotifier.new);

class BidState {
  const BidState({
    this.isSubmitting = false,
    this.errorMessage,
    this.lastResponse,
    this.clientRequestId,
  });

  final bool isSubmitting;
  final String? errorMessage;
  final PlaceBidResponseDto? lastResponse;
  final String? clientRequestId;

  BidState copyWith({
    bool? isSubmitting,
    String? errorMessage,
    PlaceBidResponseDto? lastResponse,
    String? clientRequestId,
    bool clearError = false,
  }) {
    return BidState(
      isSubmitting: isSubmitting ?? this.isSubmitting,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      lastResponse: lastResponse ?? this.lastResponse,
      clientRequestId: clientRequestId ?? this.clientRequestId,
    );
  }
}

class BidNotifier extends FamilyNotifier<BidState, int> {
  static final _random = Random.secure();

  @override
  BidState build(int lotId) => const BidState();

  String minimumBidMessage(ApiException error) {
    if (error.code != 'bid_too_low') {
      return error.message;
    }
    final minimumCents = error.details['minimumCents'];
    if (minimumCents == null) {
      return error.message;
    }
    final cents = int.tryParse(minimumCents);
    if (cents == null) {
      return error.message;
    }
    final minimum = (cents / 100).toStringAsFixed(2);
    return 'Minimum bid is \$$minimum';
  }

  String mapError(ApiException error) {
    return switch (error.code) {
      'bid_too_low' => minimumBidMessage(error),
      'bid_not_live' => 'This lot is no longer accepting bids',
      'bid_self' => 'You cannot bid on your own lot',
      'bid_conflict' => 'Another bid was placed — try again',
      'lock_timeout' => 'Auction is busy — try again',
      _ => error.message,
    };
  }

  Future<bool> submitBid(String amount) async {
    final clientRequestId = state.clientRequestId ?? _newClientRequestId();
    state = state.copyWith(isSubmitting: true, clientRequestId: clientRequestId, clearError: true);

    try {
      final response = await ref.read(bidsApiProvider).placeBid(arg, amount, clientRequestId);
      state = state.copyWith(isSubmitting: false, lastResponse: response, clearError: true);
      await ref.read(lotDetailProvider(arg).notifier).reload();
      return true;
    } on ApiException catch (error) {
      if (error.code == 'bid_too_low' ||
          error.code == 'bid_not_live' ||
          error.code == 'bid_self' ||
          error.code == 'bid_conflict' ||
          error.code == 'lock_timeout') {
        state = state.copyWith(
          isSubmitting: false,
          errorMessage: mapError(error),
          clientRequestId: clientRequestId,
        );
        return false;
      }
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: mapError(error),
        clientRequestId: clientRequestId,
      );
      rethrow;
    } catch (error) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: 'Could not place bid',
        clientRequestId: clientRequestId,
      );
      rethrow;
    }
  }

  void resetAttempt() {
    state = const BidState();
  }

  void setValidationError(String message) {
    state = state.copyWith(errorMessage: message, clearError: false);
  }

  String _newClientRequestId() {
    final bytes = List<int>.generate(16, (_) => _random.nextInt(256));
    return bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
  }
}
