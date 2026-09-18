/**
 * LoginPage — Pantalla de inicio de sesión de la student-app.
 *
 * Diseño visual: sigue el estilo del prototipo .dc.html y de register-page.tsx
 * (dark theme con fondo #0a0e0c, card #101512, acento verde #2ee88a).
 *
 * Integración con el backend:
 * - Usa `useLogin` hook → `AuthService` → `HttpApiAdapter` → `POST /api/v1/auth/login`.
 * - Maneja los 3 tipos de error del backend:
 *   · HTTP 400: errores de validación por campo (inline bajo cada input).
 *   · HTTP 401: credenciales incorrectas (banner global).
 *   · HTTP 423: cuenta bloqueada por intentos fallidos (banner global).
 * - Login exitoso → guarda tokens en localStorage → redirige a /contests.
 *
 * Backend de referencia:
 *   Endpoint: POST /api/v1/auth/login (auth-service, puerto 8081)
 *   Request:  { email: string, password: string }
 *   Response: { accessToken, refreshToken, type: "Bearer", expiresIn }
 *   Docs:     backend/services/auth-service/docs/AUTH_API.md
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ NOTA: El prototipo .dc.html define el campo como "Correo o handle",   │
 * │ pero el backend actual solo acepta email. Cuando se implemente login  │
 * │ por handle en el backend (LoginRequest.java → campo identifier +      │
 * │ búsqueda por email y username), actualizar este componente y el       │
 * │ useLogin hook para enviar el campo genérico.                          │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
"use client"

import { useState } from "react"
import Link from "next/link"
import { useLogin } from "../hooks/use-login"

export function LoginPage() {
  /** Toggle para mostrar/ocultar la contraseña en texto plano. */
  const [showPassword, setShowPassword] = useState(false)

  const {
    formData,
    errors,
    serverError,
    isLoading,
    handleChange,
    handleSubmit,
  } = useLogin()

  return (
    <div className="min-h-screen bg-[#0a0e0c] font-sans pb-[120px] pt-[80px]">
      <div className="max-w-[430px] mx-auto px-6">

        {/* ── Header: Logo + Título + Subtítulo ─────────────────────────── */}
        <div className="text-center mb-[26px]">
          <Link href="/">
            <div
              className="w-[44px] h-[44px] rounded-[13px] bg-[#2ee88a] inline-flex items-center justify-center text-[#07120c] font-mono font-bold text-[20px] mb-[16px] hover:bg-[#5cf0a8] transition-colors cursor-pointer"
              style={{ boxShadow: '0 0 22px rgba(46,232,138,0.25)' }}
            >
              &gt;_
            </div>
          </Link>
          <h1 className="m-0 text-[25px] font-[800] tracking-[-0.025em] text-[#f2fbf6]">
            Bienvenido de vuelta
          </h1>
          <p className="mt-[9px] mb-0 text-[13.5px] text-[#8fa39a] leading-[1.55]">
            Ingresa para continuar con tus maratones y envíos.
          </p>
        </div>

        {/* ── Banner de error de servidor (401 / 423 / red) ─────────────── */}
        {serverError && (
          <div className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-[13.5px] text-center font-medium">
            {serverError}
          </div>
        )}

        {/* ── Card del formulario ───────────────────────────────────────── */}
        <div className="bg-[#101512] border border-[#1f2a24] rounded-[14px] p-[26px]">
          <form onSubmit={handleSubmit}>

            {/* ── Correo electrónico ────────────────────────────────────── */}
            <div className="mb-[15px]">
              <label
                htmlFor="login-email"
                className="block text-[11px] font-bold tracking-[0.1em] uppercase text-[#6f847a] mb-[7px]"
              >
                Correo electrónico
              </label>
              <input
                id="login-email"
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="tu@email.com"
                autoComplete="email"
                className="w-full px-[13px] py-[11px] rounded-[9px] bg-[#0c110e] border border-[#253129] text-[#e6efe9] text-[13.5px] outline-none transition-all focus:border-[#2ee88a] focus:ring-4 focus:ring-[#2ee88a]/10 disabled:opacity-50"
                disabled={isLoading}
              />
              {errors.email && (
                <p className="mt-2 text-xs text-red-400">{errors.email}</p>
              )}
            </div>

            {/* ── Contraseña ────────────────────────────────────────────── */}
            <div className="mb-[15px]">
              <div className="flex items-center justify-between mb-[7px]">
                <label
                  htmlFor="login-password"
                  className="block text-[11px] font-bold tracking-[0.1em] uppercase text-[#6f847a]"
                >
                  Contraseña
                </label>
                {/*
                 * Placeholder — el backend no tiene endpoint de recuperación de contraseña.
                 * Cuando se implemente, cambiar href="#" por la ruta real.
                 */}
                <Link
                  href="#"
                  className="text-[11px] font-semibold text-[#2ee88a] hover:text-[#5cf0a8] transition-colors"
                >
                  ¿Olvidaste tu contraseña?
                </Link>
              </div>
              <div className="relative">
                <input
                  id="login-password"
                  type={showPassword ? "text" : "password"}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="••••••••"
                  autoComplete="current-password"
                  className="w-full px-[13px] py-[11px] pr-[42px] rounded-[9px] bg-[#0c110e] border border-[#253129] text-[#e6efe9] text-[13.5px] font-mono outline-none transition-all focus:border-[#2ee88a] focus:ring-4 focus:ring-[#2ee88a]/10 disabled:opacity-50"
                  disabled={isLoading}
                />
                {/* Toggle visibilidad de contraseña */}
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="absolute right-[13px] top-1/2 -translate-y-1/2 text-[#6f847a] hover:text-[#e6efe9] transition-colors"
                  tabIndex={-1}
                  aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
                >
                  {showPassword ? (
                    // Ícono: ojo cerrado (EyeOff)
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                      <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                      <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24" />
                      <line x1="1" y1="1" x2="23" y2="23" />
                    </svg>
                  ) : (
                    // Ícono: ojo abierto (Eye)
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  )}
                </button>
              </div>
              {errors.password && (
                <p className="mt-2 text-xs text-red-400">{errors.password}</p>
              )}
            </div>

            {/* ── Botón de submit ────────────────────────────────────────── */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full p-[12px] rounded-[9px] bg-[#2ee88a] text-[#06120b] text-[14px] font-bold text-center cursor-pointer transition-all hover:bg-[#5cf0a8] mt-[20px] disabled:opacity-70 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {isLoading && (
                <svg
                  className="animate-spin h-4 w-4 text-[#06120b]"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
              )}
              {isLoading ? "Iniciando sesión..." : "Iniciar sesión"}
            </button>
          </form>

          {/* ── Link a registro ──────────────────────────────────────────── */}
          <div className="text-center mt-[16px] text-[12.5px] text-[#6f847a]">
            ¿Aún no tienes cuenta?{" "}
            <Link
              href="/register"
              className="text-[#2ee88a] font-semibold cursor-pointer hover:underline"
            >
              Regístrate
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
