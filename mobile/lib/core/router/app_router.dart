import 'package:go_router/go_router.dart';

import '../../features/categories/presentation/categories_screen.dart';
import '../../features/home/presentation/home_screen.dart';

final appRouter = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const HomeScreen(),
    ),
    GoRoute(
      path: '/categories',
      builder: (context, state) => const CategoriesScreen(),
    ),
  ],
);
