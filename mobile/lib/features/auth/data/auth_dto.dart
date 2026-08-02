class UserDto {
  const UserDto({
    required this.id,
    required this.email,
    required this.displayName,
    required this.roles,
  });

  factory UserDto.fromJson(Map<String, dynamic> json) {
    return UserDto(
      id: json['id'] as int,
      email: json['email'] as String,
      displayName: json['displayName'] as String,
      roles: (json['roles'] as List<dynamic>).map((e) => e as String).toList(),
    );
  }

  final int id;
  final String email;
  final String displayName;
  final List<String> roles;
}

class AuthTokensDto {
  const AuthTokensDto({
    required this.accessToken,
    required this.refreshToken,
    required this.expiresIn,
    required this.user,
  });

  factory AuthTokensDto.fromJson(Map<String, dynamic> json) {
    return AuthTokensDto(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      expiresIn: json['expiresIn'] as int,
      user: UserDto.fromJson(json['user'] as Map<String, dynamic>),
    );
  }

  final String accessToken;
  final String refreshToken;
  final int expiresIn;
  final UserDto user;
}
