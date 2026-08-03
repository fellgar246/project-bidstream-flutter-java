class AppConfig {
  AppConfig._();

  static const apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080/api/v1',
  );

  static const appName = String.fromEnvironment(
    'APP_NAME',
    defaultValue: 'BidStream',
  );
}
