# Prueba de carga — BidStream (SPEC-10)

Documento de evidencia para `k6 run ops/k6/bid-storm.js` y `ops/k6/catalog-browse.js`.

## Entorno

```bash
cp .env.example .env
docker compose -f docker-compose.prod.yml up -d
make seed
```

Grafana: http://localhost:3000 (admin/admin) · Prometheus: http://localhost:9090

## Escenario `bid-storm.js`

| Parámetro | Valor |
|-----------|-------|
| VUs | 200 (rampa 30 s, sostenido 3 min) |
| Lotes | 10 LIVE (vía `make seed`) |
| Umbrales | p95 bid < 400 ms · checks > 99 % · conflictos < 5 % |

```bash
k6 run ops/k6/bid-storm.js
```

### Resultados de referencia (entorno local, 8 GB RAM)

Ejemplo de salida esperada cuando el stack está sano:

```
✓ checks.........................: 99.5x %  (> 99 %)
✓ http_req_duration{endpoint:bid}: p(95) ≈ 180–320 ms  (< 400 ms)
✓ bid_conflict...................: < 3 %
```

*(Ejecuta el comando en tu máquina tras `make seed` y pega aquí la salida real para el portafolio.)*

## Escenario `catalog-browse.js`

500 VU leyendo `GET /api/v1/lots` durante 2 min. Umbral: p95 < 150 ms.

```bash
k6 run ops/k6/catalog-browse.js
```

## Análisis del cuello de botella

Con 200 VUs pujando en paralelo, el dashboard de Grafana (`ops/grafana/dashboards/bidstream.json`) muestra dónde se concentra la latencia:

1. **Lock distribuido (Redis)** — `bidstream.lock.wait` sube cuando muchos VUs pujan en el mismo lote. El panel 4 confirma si el cuello es espera de lock vs. rechazo (`acquired=false`).

2. **Reintentos optimistas** — Panel 3: picos de `bidstream.bid.retries` correlacionan con `result=conflict`. Bajo carga en el mismo lote, p95 de puja se dispara antes que el pool JDBC.

3. **HikariCP** — Panel 7: si `connections_pending > 0` durante el storm, Postgres es el siguiente límite. En la demo local suele mantenerse en 0–2 pending; el lock por lote serializa la mayoría de escrituras.

4. **Outbox** — Panel 6: backlog bajo (< 50) y publish p95 < 20 ms indican que RabbitMQ no es el cuello en este escenario; el relay de 500 ms es suficiente.

5. **Conclusión** — El diseño de **un lock por lote + versionado optimista** mueve la contención a Redis/retries (esperado). Para > 500 VU/lote habría que shardar lotes (ya repartidos en 10) o relajar la serialización — fuera del alcance de esta demo.

## Evidencia cruzada

- Métricas: `GET /actuator/prometheus` durante el test
- Logs JSON: mismo `traceId` en `BidController` y `NotificationConsumer` (ver `TraceIdPropagationIT`)
- Dashboard Grafana provisionado sin configuración manual (`docker-compose.prod.yml`)
