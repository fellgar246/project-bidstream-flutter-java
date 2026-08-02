#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "==> Backend: format, lint, tests, coverage"
cd backend
./gradlew spotlessCheck checkstyleMain checkstyleTest test checkAll
cd ..

echo "==> Flutter: analyze and tests"
cd mobile
flutter pub get
flutter analyze
flutter test
cd ..

echo "==> All checks passed"
