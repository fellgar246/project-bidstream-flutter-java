enum AuthStatus { unknown, authenticated, unauthenticated }

class AuthState {
  const AuthState._(this.status, this.user);

  const AuthState.unknown() : this._(AuthStatus.unknown, null);

  const AuthState.authenticated(UserSnapshot user)
    : this._(AuthStatus.authenticated, user);

  const AuthState.unauthenticated() : this._(AuthStatus.unauthenticated, null);

  final AuthStatus status;
  final UserSnapshot? user;
}

class UserSnapshot {
  const UserSnapshot({
    required this.id,
    required this.email,
    required this.displayName,
    required this.roles,
  });

  final int id;
  final String email;
  final String displayName;
  final List<String> roles;

  bool get isSeller => roles.contains('SELLER');
}
