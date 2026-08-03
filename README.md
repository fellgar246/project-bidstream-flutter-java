# BidStream

Plataforma de subastas en vivo — monorepo con backend Spring Boot (arquectura hexagonal)
y app Flutter.

## Requisitos

- Java 21+
- Docker y Docker Compose
- Flutter 3.2x / Dart 3
- Gradle (wrapper incluido en `backend/`)

## Infraestructura local

```bash
cp .env.example .env
docker compose up -d
```

Servicios:

| Servicio   | Puerto(s)     | UI / notas              |
|------------|---------------|-------------------------|
| PostgreSQL | 5433 (host; evita conflicto con Postgres local en 5432) | — |
| Redis      | 6379          | —                       |
| RabbitMQ   | 5672, 15672   | http://localhost:15672  |
| MinIO      | 9000, 9001    | http://localhost:9001   |
| MailHog    | 1025, 8025    | http://localhost:8025   |

Verifica salud: `docker compose ps` (todos `healthy`). En MinIO debe existir el bucket `bidstream`.

## Backend

```bash
cd backend
./gradlew bootRun
```

Endpoints:

- `GET http://localhost:8080/api/v1/health`
- `GET http://localhost:8080/api/v1/categories`

## App móvil

Emulador Android (API contra host):

```bash
cd mobile
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

iOS simulator / dispositivo físico: usa la IP de tu máquina en lugar de `10.0.2.2`.

## Calidad

```bash
./scripts/check.sh
```

Ejecuta Spotless, Checkstyle, tests backend (con JaCoCo ≥ 80 % en `domain`/`application`)
y tests Flutter.

## Estructura

```
backend/   → domain, application, infrastructure, api
mobile/    → Flutter (Riverpod, go_router, Dio)
plans/     → specs y plan maestro
scripts/   → check.sh
```

Documentación de aprendizaje: [`CONCEPTOS.md`](CONCEPTOS.md).

## Tiempo real (SPEC-06)

- WebSocket STOMP en `GET /ws?token=<accessToken>` (heartbeat 10 s).
- Topics: `/topic/lots/{id}`, presencia en `/topic/lots/{id}/presence`.
- Recuperación: `GET /api/v1/lots/{id}/events?afterEventId=...`
- **Límite conocido:** broker STOMP en memoria ⇒ despliegue de una sola instancia del API para subastas en vivo.
