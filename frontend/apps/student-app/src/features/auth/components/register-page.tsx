"use client"

import { useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { createHttpApi } from "@/core/api"
import { Logo } from "@/shared/ui/logo"
import { Button } from "@/shared/ui/primitives/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/shared/ui/primitives/card"
import { Input } from "@/shared/ui/primitives/input"
import { Label } from "@/shared/ui/primitives/label"
import { Checkbox } from "@/shared/ui/primitives/checkbox"
import { Building2, Eye, EyeOff, Github, Mail, User, ArrowLeft, Check } from "lucide-react"

export function RegisterPage() {
  const router = useRouter()
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const [institutionError, setInstitutionError] = useState<string | null>(null)
  const [acceptTerms, setAcceptTerms] = useState(false)

  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    username: "",
    email: "",
    institution: "",
    password: "",
  })

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target
    setFormData(prev => ({ ...prev, [id]: value }))
    if (serverError) setServerError(null)
    if (id === "institution" && institutionError) setInstitutionError(null)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)
    setServerError(null)
    setInstitutionError(null)

    if (!acceptTerms) {
      setServerError("Debes aceptar los Términos de Servicio y la Política de Privacidad para continuar.")
      setIsLoading(false)
      return
    }

    // La institución es opcional: si queda vacía no se envía la clave (el backend la guarda como null).
    const institution = formData.institution.trim()

    try {
      const response = await createHttpApi().post<Record<string, unknown> | null>("/v1/auth/register", {
        firstName: formData.firstName,
        lastName: formData.lastName,
        username: formData.username,
        email: formData.email,
        password: formData.password,
        ...(institution ? { institution } : {}),
      })

      if (!response.ok) {
        const errorData = response.data
        console.error("Backend error response:", errorData)
        if (typeof errorData?.institution === "string") {
          // Error de validación de campo: se muestra junto al campo institución.
          setInstitutionError(errorData.institution)
          const otherMessages = Object.entries(errorData)
            .filter(([field, value]) => field !== "institution" && typeof value === "string")
            .map(([, value]) => value as string)
          setServerError(otherMessages.length > 0 ? otherMessages.join(" ") : null)
        } else {
          setServerError(
            (typeof errorData?.message === "string" && errorData.message) ||
            (typeof errorData?.error === "string" && errorData.error) ||
            (errorData ? JSON.stringify(errorData) : "Ocurrió un error al registrarse. Verifica tus datos.")
          )
        }
        setIsLoading(false)
        return
      }

      // Registro exitoso, redirigir al login
      setIsLoading(false)
      router.push("/login?registered=true")
    } catch (error: any) {
      console.error("Error en registro:", error)
      setServerError("Hubo un error de conexión con el servidor. Por favor, intenta de nuevo.")
      setIsLoading(false)
    }
  }

  const passwordRequirements = [
    { met: formData.password.length >= 8, text: "Mínimo 8 caracteres" },
    { met: /[A-Z]/.test(formData.password), text: "Una mayúscula" },
    { met: /[0-9]/.test(formData.password), text: "Un número" },
    { met: /[^A-Za-z0-9]/.test(formData.password), text: "Un caracter especial" },
  ]

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative py-12">
      {/* Background Effects */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-primary/5 via-background to-background" />
      
      <div className="relative w-full max-w-md">
        {/* Back to Home */}
        <Link 
          href="/" 
          className="inline-flex items-center gap-2 text-muted-foreground hover:text-foreground mb-8 transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          Volver al inicio
        </Link>

        <Card className="border-border bg-card shadow-md">
          <CardHeader className="text-center pb-2">
            <div className="flex justify-center mb-4">
              <Link href="/" className="flex items-center rounded-xl transition-opacity hover:opacity-80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
                <Logo variant="mark" className="h-12 w-12" />
              </Link>
            </div>
            <CardTitle className="text-2xl font-bold text-foreground">
              Crea tu cuenta
            </CardTitle>
            <CardDescription className="text-muted-foreground">
              Únete a la comunidad de Goslint Judge
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4 pt-4">
            {serverError && (
              <div className="p-3 rounded-md bg-destructive/10 border border-destructive/30 text-destructive text-sm text-center">
                {serverError}
              </div>
            )}
            
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="firstName" className="text-foreground">
                    Nombre
                  </Label>
                  <Input
                    id="firstName"
                    type="text"
                    placeholder="Juan"
                    className="bg-background border-input"
                    value={formData.firstName}
                    onChange={handleChange}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="lastName" className="text-foreground">
                    Apellido
                  </Label>
                  <Input
                    id="lastName"
                    type="text"
                    placeholder="Pérez"
                    className="bg-background border-input"
                    value={formData.lastName}
                    onChange={handleChange}
                    required
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="username" className="text-foreground">
                  Nombre de Usuario
                </Label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="username"
                    type="text"
                    placeholder="juanperez_dev"
                    className="pl-10 bg-background border-input"
                    value={formData.username}
                    onChange={handleChange}
                    required
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="email" className="text-foreground">
                  Correo Electrónico
                </Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="email"
                    type="email"
                    placeholder="tu@email.com"
                    className="pl-10 bg-background border-input"
                    value={formData.email}
                    onChange={handleChange}
                    required
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="institution" className="text-foreground">
                  Institución <span className="text-muted-foreground font-normal">(opcional)</span>
                </Label>
                <div className="relative">
                  <Building2 className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="institution"
                    type="text"
                    maxLength={255}
                    placeholder="Universidad, colegio u organización"
                    className="pl-10 bg-background border-input"
                    value={formData.institution}
                    onChange={handleChange}
                    aria-invalid={institutionError ? true : undefined}
                    aria-describedby={institutionError ? "institution-error" : undefined}
                  />
                </div>
                {institutionError && (
                  <p id="institution-error" className="text-xs text-destructive">
                    {institutionError}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="password" className="text-foreground">
                  Contraseña
                </Label>
                <div className="relative">
                  <Input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    placeholder="••••••••"
                    className="pr-10 bg-background border-input"
                    value={formData.password}
                    onChange={handleChange}
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
                {/* Password Requirements */}
                {formData.password && (
                  <div className="grid grid-cols-2 gap-1 mt-2">
                    {passwordRequirements.map((req, i) => (
                      <div key={i} className="flex items-center gap-1.5 text-xs">
                        <Check className={`h-3 w-3 ${req.met ? 'text-primary' : 'text-muted-foreground'}`} />
                        <span className={req.met ? 'text-primary' : 'text-muted-foreground'}>
                          {req.text}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="flex items-start gap-2">
                <Checkbox 
                  id="terms" 
                  checked={acceptTerms}
                  onCheckedChange={(checked) => setAcceptTerms(checked === true)}
                  className="mt-1 border-border data-[state=checked]:bg-primary data-[state=checked]:border-primary" 
                />
                <Label htmlFor="terms" className="text-sm text-muted-foreground font-normal leading-tight">
                  Acepto los{" "}
                  <Link href="#" className="text-primary hover:text-primary/80 transition-colors">
                    Términos de Servicio
                  </Link>{" "}
                  y la{" "}
                  <Link href="#" className="text-primary hover:text-primary/80 transition-colors">
                    Política de Privacidad
                  </Link>
                </Label>
              </div>

              <Button
                type="submit"
                className="w-full bg-primary text-primary-foreground hover:bg-primary/90 glow-green-sm"
                disabled={isLoading}
              >
                {isLoading ? "Creando cuenta..." : "Crear Cuenta"}
              </Button>
            </form>

            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-border" />
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-card px-2 text-muted-foreground">
                  O regístrate con
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
              ¿Ya tienes una cuenta?{" "}
              <Link href="/login" className="text-primary hover:text-primary/80 font-medium transition-colors">
                Inicia Sesión
              </Link>
            </p>
          </CardFooter>
        </Card>
      </div>
    </div>
  )
}
