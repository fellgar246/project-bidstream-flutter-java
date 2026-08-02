class ApiException implements Exception {
  ApiException({
    required this.code,
    required this.message,
    required this.details,
    this.traceId,
  });

  factory ApiException.fromJson(Map<String, dynamic> json) {
    final error = json['error'] as Map<String, dynamic>? ?? {};
    final rawDetails = error['details'];
    final details = <String, String>{};
    if (rawDetails is Map) {
      rawDetails.forEach((key, value) {
        details['$key'] = '$value';
      });
    }
    return ApiException(
      code: error['code'] as String? ?? 'unknown_error',
      message: error['message'] as String? ?? 'Unexpected error',
      details: details,
      traceId: json['traceId'] as String?,
    );
  }

  final String code;
  final String message;
  final Map<String, String> details;
  final String? traceId;

  @override
  String toString() => 'ApiException(code: $code, message: $message)';
}
