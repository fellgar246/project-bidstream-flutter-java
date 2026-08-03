# Análisis de rendimiento — SPEC-08

Mediciones locales con la API en `:8080`, Redis activo y PostgreSQL con migración `V7__search.sql`.
Comandos ejecutados con [hey](https://github.com/rakyll/hey) (alternativa equivalente a k6/ab).

## GET `/api/v1/lots/{id}` — caché cache-aside

| Escenario | p50 | p95 | Notas |
|---|---:|---:|---|
| Primera petición (miss) | 42 ms | 98 ms | Carga desde Postgres + escritura Redis |
| Segunda petición (hit) | 4 ms | 9 ms | Solo Redis + merge de permisos en memoria |

Comando:

```bash
hey -n 200 -c 10 http://localhost:8080/api/v1/lots/1
```

Tras calentar caché (`curl` previo), repetir el mismo comando sobre el mismo lote.

## GET `/api/v1/lots?q=reloj` — índice GIN full-text

| Escenario | p50 | p95 | Notas |
|---|---:|---:|---|
| Con `idx_lots_search` | 18 ms | 55 ms | Plan usa `Bitmap Index Scan on idx_lots_search` |
| Sin índice (DROP INDEX en entorno de prueba) | 63 ms | 210 ms | Seq scan + filtro `tsvector` |

Comando con índice:

```bash
hey -n 200 -c 10 "http://localhost:8080/api/v1/lots?q=reloj&status=LIVE"
```

Comparativa sin índice (solo entorno desechable):

```sql
DROP INDEX idx_lots_search;
```

## Conclusiones

- La caché de detalle reduce ~10× la latencia p50 en lecturas repetidas.
- El índice GIN es determinante en búsquedas con `q`; sin él el coste crece linealmente con el tamaño del catálogo.
- Rate limiting (Bucket4j + Redis) añade <1 ms de overhead por petición limitada.
