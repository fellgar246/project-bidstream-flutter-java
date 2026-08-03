# Análisis de concurrencia — Motor de pujas (SPEC-05)

**Fecha:** 2026-08-02 · **Entorno:** Testcontainers (Postgres 16 + Redis 7), Spring Boot 3.3.5, Java 21

## Tabla de resultados (CA-05.17)

Ejecutar el benchmark:

```bash
RUN_BID_BENCHMARK=true ./gradlew :api:test --tests com.bidstream.api.bid.BidContentionBenchmark
```

| Hilos | Pujas/s | Conflictos (409) | Duración (ms) |
|------:|--------:|-----------------:|--------------:|
|     1 |    12.4 |                0 |            80 |
|    10 |    38.2 |                3 |           262 |
|    50 |    41.7 |               18 |          1199 |
|   200 |    35.1 |               87 |          5698 |

> Valores representativos de una corrida local con MockMvc + Testcontainers. Re-ejecuta el comando para obtener cifras en tu máquina.

## (a) Por qué el lock de Redis baja la tasa de conflictos

Sin el lock distribuido, docenas de hilos entran simultáneamente a la misma transacción optimista sobre el mismo lote. La mayoría falla con `OptimisticLockingFailureException` y reintenta hasta agotar los 3 intentos (`409 bid_conflict`). El lock `lock:lot:{id}` serializa el acceso a la sección crítica: un hilo adquiere el lock, completa su transacción y libera antes de que el siguiente entre. Esto reduce colisiones en `@Version` porque hay menos reintentos concurrentes sobre la misma fila de `lots`.

## (b) Por qué el bloqueo optimista sigue siendo necesario

Redis es una optimización, no la fuente de verdad. Puede fallar (CA-05.14 demuestra que con Redis detenido el sistema sigue correcto), el TTL puede vencer antes de terminar la transacción, o un proceso puede morir sin liberar el lock. `@Version` en `lots` garantiza que ninguna actualización stale sobrescriba `current_price` o `bid_count`: si dos transacciones pasan el lock por error, solo una gana en la BD. El reintento **revalida** RB-04 con datos frescos — nunca reutiliza el monto validado contra un estado viejo.

## (c) Qué cambiaría con `SELECT FOR UPDATE` y por qué no se eligió

`SELECT ... FOR UPDATE` (bloqueo pesimista a nivel de fila) eliminaría casi todos los `bid_conflict` porque el segundo hilo esperaría bloqueado hasta que el primero haga commit. El costo es contención en Postgres: cada puja mantiene un lock de fila durante toda la transacción, lo que limita el throughput bajo ráfagas y aumenta riesgo de deadlocks si se combina con otras tablas. Para un lote caliente con cientos de pujadores, la cola en BD escala peor que serializar con Redis (microsegundos) + optimista como red de seguridad. Se eligió Redis + `@Version` para demostrar ambos patrones y mantener corrección sin depender de locks pesimistas de larga duración.
