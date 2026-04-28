/**
 * ═══════════════════════════════════════════════════════════════════
 *  load-test.js  –  Prueba de Carga con k6
 *  Sistema de Decanato – OrtizSerranoTP3
 * ═══════════════════════════════════════════════════════════════════
 *
 *  INSTALACIÓN:
 *    # Ubuntu/Debian
 *    sudo gpg -k
 *    sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
 *      --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
 *    echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
 *      | sudo tee /etc/apt/sources.list.d/k6.list
 *    sudo apt-get update && sudo apt-get install k6
 *
 *  EJECUCIÓN:
 *    # Asegurarse de que el backend está corriendo en localhost:8080
 *    # Modo humo (1 usuario, 30s):
 *    k6 run --env ESCENARIO=smoke load-test.js
 *
 *    # Carga normal (50 usuarios, 2 min):
 *    k6 run --env ESCENARIO=normal load-test.js
 *
 *    # Pico de tráfico (200 usuarios, 1 min):
 *    k6 run --env ESCENARIO=pico load-test.js
 *
 *    # Estrés progresivo (ramp-up hasta 300 usuarios):
 *    k6 run --env ESCENARIO=estres load-test.js
 *
 *    # Con reporte HTML (requiere k6-reporter):
 *    k6 run --out json=resultados.json load-test.js
 *
 *  UMBRALES (thresholds):
 *    - P95 de latencia ≤ 500ms
 *    - Tasa de errores HTTP < 5%
 *    - P99 de inscripciones ≤ 1000ms
 * ═══════════════════════════════════════════════════════════════════
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// ── URL base ──────────────────────────────────────────────────────────────────
const BASE = 'http://localhost:8080/api';

// ── Credenciales admin (seeded por DataInitializer) ───────────────────────────
const ADMIN_EMAIL    = 'admin@decanato.edu';
const ADMIN_PASSWORD = 'Admin1234';

// ── Métricas personalizadas ───────────────────────────────────────────────────
const errorRate          = new Rate('errores_http');
const inscripcionLatency = new Trend('latencia_inscripcion_ms', true);
const inscripcionesOk    = new Counter('inscripciones_exitosas');
const inscripcionesEnCola= new Counter('inscripciones_encoladas');
const rateLimitHits      = new Counter('rate_limit_429');

// ── Escenarios de carga ───────────────────────────────────────────────────────
const ESCENARIO = __ENV.ESCENARIO || 'normal';

const escenarios = {
  /** Humo: 1 usuario, 30s – verifica que el sistema funciona */
  smoke: {
    executor: 'constant-vus',
    vus: 1,
    duration: '30s',
  },
  /** Carga normal: rampa suave hasta 50 usuarios, meseta de 2 min */
  normal: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '30s', target: 20  },  // calentamiento
      { duration: '2m',  target: 50  },  // carga sostenida
      { duration: '30s', target: 0   },  // enfriamiento
    ],
  },
  /** Pico de tráfico: simula período de inscripciones masivas */
  pico: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '10s', target: 100 },  // subida abrupta
      { duration: '1m',  target: 200 },  // pico máximo
      { duration: '10s', target: 50  },  // caída parcial
      { duration: '30s', target: 0   },  // fin
    ],
  },
  /** Estrés: llevar al sistema hasta el límite */
  estres: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '1m',  target: 50  },
      { duration: '1m',  target: 100 },
      { duration: '1m',  target: 200 },
      { duration: '1m',  target: 300 },
      { duration: '1m',  target: 0   },
    ],
  },
};

// ── Configuración exportada ───────────────────────────────────────────────────
export const options = {
  scenarios: {
    carga: escenarios[ESCENARIO],
  },
  thresholds: {
    // Latencia general
    http_req_duration:          ['p(95)<500', 'p(99)<1000'],
    // Tasa de errores HTTP (5xx) menor al 5%
    errores_http:               ['rate<0.05'],
    // Latencia específica de inscripciones
    latencia_inscripcion_ms:    ['p(95)<800'],
  },
};

// ── Variables de VU (compartidas dentro del mismo usuario virtual) ────────────
let adminToken   = null;
let materiaId    = null;
let estudianteId = null;
let estToken     = null;

// ── Setup global: corre 1 vez antes de todos los VUs ─────────────────────────
export function setup() {
  // Login de admin
  const loginRes = http.post(`${BASE}/auth/login`,
    JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(loginRes, { 'admin login 200': (r) => r.status === 200 });

  const token = loginRes.json('token');
  const authH = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` };

  // Crear materia de prueba con 100 cupos
  const matRes = http.post(`${BASE}/admin/materias`,
    JSON.stringify({
      codigo: 'LOAD-TEST-01',
      nombre: 'Materia Load Test',
      cuposMaximos: 100,
    }),
    { headers: authH }
  );
  check(matRes, { 'materia creada': (r) => r.status === 201 || r.status === 400 });

  let idMateria = null;
  if (matRes.status === 201) {
    idMateria = matRes.json('idMateria');
  }

  return { token, idMateria };
}

// ── Función principal: cada VU la ejecuta continuamente ──────────────────────
export default function (data) {
  const adminH = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  // ── Grupo 1: Login de estudiante ─────────────────────────────────────────
  group('1_login_estudiante', function () {
    const email    = `loadtest.vu${__VU}.iter${__ITER}@test.com`;
    const password = 'LoadTest12';

    // Crear estudiante (puede fallar si ya existe, es OK)
    http.post(`${BASE}/admin/usuarios`,
      JSON.stringify({ nombre: 'Load', apellido: 'Test',
        email, password, rol: 'ESTUDIANTE' }),
      { headers: adminH }
    );

    // Login del estudiante creado
    const loginRes = http.post(`${BASE}/auth/login`,
      JSON.stringify({ email, password }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    const loginOk = check(loginRes, {
      'login estudiante 200': (r) => r.status === 200,
      'token presente':       (r) => r.json('token') !== undefined,
    });
    errorRate.add(!loginOk);

    if (!loginOk) return;
    estToken = loginRes.json('token');
  });

  sleep(0.2);

  // ── Grupo 2: Inscripción (endpoint crítico) ───────────────────────────────
  if (estToken && data.idMateria) {
    group('2_inscripcion', function () {
      const start = Date.now();
      const res = http.post(`${BASE}/inscripciones`,
        JSON.stringify({ idMateria: data.idMateria }),
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${estToken}`,
          },
        }
      );
      const duracion = Date.now() - start;
      inscripcionLatency.add(duracion);

      if (res.status === 201) {
        inscripcionesOk.add(1);
      } else if (res.status === 202) {
        inscripcionesEnCola.add(1);
      } else if (res.status === 429) {
        rateLimitHits.add(1);
      }

      const ok = check(res, {
        'inscripcion exitosa o encolada': (r) =>
          r.status === 201 || r.status === 202 || r.status === 400 || r.status === 429,
      });
      errorRate.add(res.status >= 500);
    });
  }

  sleep(0.3);

  // ── Grupo 3: Consulta de materias ─────────────────────────────────────────
  group('3_listar_materias', function () {
    const res = http.get(`${BASE}/estudiante/materias`, {
      headers: {
        'Authorization': `Bearer ${estToken || data.token}`,
      },
    });
    const ok = check(res, {
      'materias 200': (r) => r.status === 200,
    });
    errorRate.add(!ok);
  });

  sleep(0.5);

  // ── Grupo 4: Health check ─────────────────────────────────────────────────
  group('4_health_check', function () {
    const res = http.get(`${BASE}/health`);
    check(res, {
      'health UP': (r) => r.status === 200 && r.json('status') === 'UP',
    });
  });

  sleep(1);
}

// ── Teardown: resumen post-ejecución ─────────────────────────────────────────
export function teardown(data) {
  console.log('═'.repeat(60));
  console.log('  RESUMEN PRUEBA DE CARGA – Decanato OrtizSerrano');
  console.log('═'.repeat(60));
  console.log(`  Escenario ejecutado : ${ESCENARIO}`);
  console.log(`  URL base            : ${BASE}`);
  console.log('  Verificar resultados en el reporte de k6 ↑');
  console.log('═'.repeat(60));
}
