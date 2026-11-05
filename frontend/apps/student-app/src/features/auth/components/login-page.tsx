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
import { Button } from "@/shared/ui/primitives/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/shared/ui/primitives/card"
import { Input } from "@/shared/ui/primitives/input"
import { Label } from "@/shared/ui/primitives/label"
import { Code2, Eye, EyeOff, Github, Mail, ArrowLeft, Loader2 } from "lucide-react"

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
    <div className="min-h-screen flex items-center justify-center p-4 relative py-12">
      {/* Background Effects */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-primary/10 via-background to-background" />
      <div className="absolute inset-0 bg-[url('data:image/svg+xml,%3Csvg%20width%3D%2260%22%20height%3D%2260%22%20viewBox%3D%220%200%2060%2060%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Cg%20fill%3D%22none%22%20fill-rule%3D%22evenodd%22%3E%3Cg%20fill%3D%22%2300ff88%22%20fill-opacity%3D%220.03%22%3E%3Ccircle%20cx%3D%221%22%20cy%3D%221%22%20r%3D%221%22%2F%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E')] opacity-50" />
      
      <div className="relative w-full max-w-md">
        {/* Back to Home */}
        <Link 
          href="/" 
          className="inline-flex items-center gap-2 text-muted-foreground hover:text-foreground mb-8 transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          Volver al inicio
        </Link>

        <Card className="border-border bg-card/80 backdrop-blur-xl shadow-2xl">
          <CardHeader className="text-center pb-2">
            <div className="flex justify-center mb-4">
              <Link href="/" className="flex items-center gap-2 group">
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10 border border-primary/30 group-hover:glow-green-sm transition-all">
                  <Code2 className="h-7 w-7 text-primary" />
                </div>
              </Link>
            </div>
            <CardTitle className="text-2xl font-bold text-foreground">
              Bienvenido de vuelta
            </CardTitle>
            <CardDescription className="text-muted-foreground">
              Ingresa para continuar con tus maratones y envíos.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4 pt-4">
            {serverError && (
              <div className="p-3 rounded-md bg-red-500/10 border border-red-500/20 text-red-500 text-sm text-center">
                {serverError}
              </div>
            )}
            
            <form onSubmit={handleSubmit} method="POST" action="#" className="space-y-4">
              
              <div className="space-y-2">
                <Label htmlFor="email" className="text-foreground">
                  Correo Electrónico
                </Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="email"
                    name="email"
                    type="email"
                    placeholder="tu@email.com"
                    autoComplete="email"
                    className="pl-10 bg-input border-border focus:border-primary focus:ring-primary"
                    value={formData.email}
                    onChange={handleChange}
                    disabled={isLoading}
                    required
                  />
                </div>
                {errors.email && (
                  <p className="text-xs text-red-400 mt-1">{errors.email}</p>
                )}
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="password" className="text-foreground">
                    Contraseña
                  </Label>
                  <Link
                    href="#"
                    className="text-xs font-semibold text-primary hover:text-primary/80 transition-colors"
                  >
                    ¿Olvidaste tu contraseña?
                  </Link>
                </div>
                <div className="relative">
                  <Input
                    id="password"
                    name="password"
                    type={showPassword ? "text" : "password"}
                    placeholder="••••••••"
                    autoComplete="current-password"
                    className="pr-10 font-mono bg-input border-border focus:border-primary focus:ring-primary"
                    value={formData.password}
                    onChange={handleChange}
                    disabled={isLoading}
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                  >
                    {showPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
                {errors.password && (
                  <p className="text-xs text-red-400 mt-1">{errors.password}</p>
                )}
              </div>

              <Button
                type="submit"
                className="w-full bg-primary text-primary-foreground hover:bg-primary/90 glow-green-sm"
                disabled={isLoading}
              >
                {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {isLoading ? "Iniciando sesión..." : "Iniciar Sesión"}
              </Button>
            </form>

            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-border" />
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-card px-2 text-muted-foreground">
                  O inicia sesión con
                </span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <Button variant="outline" className="border-border hover:border-primary hover:bg-primary/10">
                <Github className="mr-2 h-4 w-4" />
                GitHub
              </Button>
              <Button variant="outline" className="border-border hover:border-primary hover:bg-primary/10">
                <svg className="mr-2 h-4 w-4" viewBox="0 0 24 24">
                  <path
                    fill="currentColor"
                    d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                  />
                  <path
                    fill="currentColor"
                    d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                  />
                  <path
                    fill="currentColor"
                    d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                  />
                  <path
                    fill="currentColor"
                    d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                  />
                </svg>
                Google
              </Button>
            </div>
          </CardContent>

          <CardFooter className="flex justify-center border-t border-border pt-6">
            <p className="text-sm text-muted-foreground">
              ¿Aún no tienes cuenta?{" "}
              <Link href="/register" className="text-primary hover:text-primary/80 font-medium transition-colors">
                Regístrate
              </Link>
            </p>
          </CardFooter>
        </Card>
      </div>
    </div>
  )
}
