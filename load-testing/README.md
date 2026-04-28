# Pruebas de Carga – Sistema de Decanato

Herramienta: **[k6](https://k6.io/)** (open-source, sin UI requerida)

---

## Instalación de k6

```bash
# Ubuntu/Debian
sudo gpg --no-default-keyring \
  --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 \
  --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69

echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] \
https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list

sudo apt-get update && sudo apt-get install k6
```

---

## Pre-requisito

El backend debe estar corriendo antes de ejecutar:

```bash
cd OrtizSerranoTP3
./gradlew bootRun
```

---

## Escenarios disponibles

| Escenario | Usuarios | Duración | Propósito |
|-----------|----------|----------|-----------|
| `smoke`   | 1 VU     | 30s      | Verificar que el sistema responde (humo) |
| `normal`  | hasta 50 VU | ~3min | Carga de uso normal |
| `pico`    | hasta 200 VU | ~2min | Simular período de inscripciones masivas |
| `estres`  | hasta 300 VU | 5min | Llevar al sistema al límite |

---

## Ejecución

```bash
cd load-testing

# Humo (1 usuario)
k6 run --env ESCENARIO=smoke load-test.js

# Carga normal
k6 run --env ESCENARIO=normal load-test.js

# Pico de tráfico (período de inscripciones)
k6 run --env ESCENARIO=pico load-test.js

# Estrés progresivo
k6 run --env ESCENARIO=estres load-test.js

# Con salida JSON para análisis posterior
k6 run --env ESCENARIO=normal --out json=resultados.json load-test.js
```

---

## Umbrales (thresholds)

El test **FALLA** si se supera alguno de estos umbrales:

| Métrica | Umbral |
|---------|--------|
| `http_req_duration` P95 | ≤ 500ms |
| `http_req_duration` P99 | ≤ 1000ms |
| `errores_http` (5xx) | < 5% |
| `latencia_inscripcion_ms` P95 | ≤ 800ms |

---

## Métricas personalizadas

| Métrica | Descripción |
|---------|-------------|
| `inscripciones_exitosas` | Cantidad de POST /inscripciones → 201 |
| `inscripciones_encoladas` | Cantidad de POST /inscripciones → 202 (cola virtual) |
| `rate_limit_429` | Cantidad de solicitudes bloqueadas por Rate Limiter |
| `latencia_inscripcion_ms` | Latencia específica del endpoint de inscripción |

---

## Interpretación de resultados

```
✓ login estudiante 200 ..........: 100% ✓ 3000  ✗ 0
✓ inscripcion exitosa o encolada .: 98%  ✓ 2940  ✗ 60
✓ health UP ......................: 100% ✓ 3000  ✗ 0

inscripciones_exitosas......: 847
inscripciones_encoladas.....: 2093
rate_limit_429..............: 312

http_req_duration............: avg=123ms  p(95)=387ms  p(99)=721ms
latencia_inscripcion_ms......: avg=201ms  p(95)=612ms
```

- **inscripciones_exitosas** debe ser ≤ cuposMaximos de la materia de prueba (100)
- **rate_limit_429** confirma que el Rate Limiter está activo bajo carga
- **inscripciones_encoladas** confirma que la cola virtual FIFO funciona bajo presión
