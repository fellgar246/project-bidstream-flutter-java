class CategoryDto {
  const CategoryDto({
    required this.id,
    required this.slug,
    required this.name,
    required this.children,
  });

  factory CategoryDto.fromJson(Map<String, dynamic> json) {
    final childrenJson = json['children'] as List<dynamic>? ?? [];
    return CategoryDto(
      id: json['id'] as int,
      slug: json['slug'] as String,
      name: json['name'] as String,
      children: childrenJson
          .map((child) => CategoryDto.fromJson(child as Map<String, dynamic>))
          .toList(),
    );
  }

  final int id;
  final String slug;
  final String name;
  final List<CategoryDto> children;
}
