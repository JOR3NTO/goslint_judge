/**
 * Modo mock (NEXT_PUBLIC_USE_MOCK=true): API simulada para ver y probar el flujo del
 * estudiante sin backend. Documentación en frontend/README.md, sección "Modo demo (mock)".
 *
 * Las features NO importan este módulo: obtienen el servidor mock a través de
 * `createHttpApi()` (core/api/http-client.ts).
 */
export { MockServer, getMockServer, resetMockData } from "./server"
export type { MockServerOptions } from "./server"
export {
  DEMO_CONTEST_PASSWORD,
  DEMO_STUDENT_EMAIL,
  DEMO_STUDENT_PASSWORD,
  MOCK_STORAGE_KEY,
} from "./model"
