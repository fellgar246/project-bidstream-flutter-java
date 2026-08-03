import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

String formatMoney(Decimal amount, Locale locale) {
  final major = amount.toDouble();
  if (locale.languageCode == 'es') {
    return NumberFormat.currency(locale: 'es_ES', symbol: '€', decimalDigits: 2).format(major);
  }
  return NumberFormat.currency(locale: 'en_US', symbol: r'$', decimalDigits: 2).format(major);
}

String _currencyName(Locale locale) {
  return switch (locale.languageCode) {
    'es' => 'EUR',
    _ => 'USD',
  };
}

String formatMoneyFromString(String amount, Locale locale) {
  return formatMoney(Decimal.parse(amount), locale);
}

String formatRelativeAge(Duration age, Locale locale) {
  if (age.inHours >= 1) {
    return '${age.inHours} h';
  }
  if (age.inMinutes >= 1) {
    return '${age.inMinutes} min';
  }
  return '${age.inSeconds} s';
}

String formatCountdown(Duration remaining, Locale locale, String Function(int h, int m) hoursFn,
    String Function(int m, int s) minutesFn) {
  final hours = remaining.inHours;
  final minutes = remaining.inMinutes.remainder(60);
  final seconds = remaining.inSeconds.remainder(60);
  if (hours > 0) {
    return hoursFn(hours, minutes);
  }
  return minutesFn(minutes, seconds);
}
