"use client"

import { useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"


export function RegisterPage() {
  const router = useRouter()
  const [isLoading, setIsLoading] = useState(false)
  const [formData, setFormData] = useState({
    fullName: "",
    email: "",
    username: "",
    password: "",
  })
  
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [serverError, setServerError] = useState<string | null>(null)

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
    // Clear field error when typing
    if (errors[name]) {
      setErrors(prev => {
        const newErrors = { ...prev }
        delete newErrors[name]
        return newErrors
      })
    }
    if (serverError) setServerError(null)
  }

  const validate = () => {
    const newErrors: Record<string, string> = {}
    
    if (!formData.fullName.trim()) {
      newErrors.fullName = "El nombre completo es obligatorio."
    }
    
    if (!formData.email.trim()) {
      newErrors.email = "El correo es obligatorio."
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = "Debes ingresar un correo válido."
    }

    if (!formData.username.trim()) {
      newErrors.username = "El handle es obligatorio."
    }

    if (!formData.password) {
      newErrors.password = "La contraseña es obligatoria."
    } else if (formData.password.length < 8) {
      newErrors.password = "Debe tener al menos 8 caracteres."
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!validate()) return

    setIsLoading(true)
    setServerError(null)

    // Simulate API call
    setTimeout(() => {
      // Simulate checking for duplicate email
      if (formData.email === "admin@uceva.edu.co" || formData.email === "admin@gmail.com") {
        setServerError("Ya existe una cuenta con este correo electrónico.")
        setIsLoading(false)
        return
      }

      // Simulate random server error
      if (Math.random() < 0.2) {
        setServerError("Hubo un error de conexión con el servidor. Por favor, intenta de nuevo.")
        setIsLoading(false)
        return
      }

      // Success! Auto-login and redirect
      setIsLoading(false)
      // Redirect to home/dashboard
      router.push("/contests")
    }, 2000)
  }

  return (
    <div className="min-h-screen bg-[#0a0e0c] font-sans pb-[120px] pt-[80px]">
      <div className="max-w-[430px] mx-auto px-6">
        
        {/* Header */}
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
            Crea tu cuenta
          </h1>
          <p className="mt-[9px] mb-0 text-[13.5px] text-[#8fa39a] leading-[1.55]">
            Únete al grupo de estudio y compite en las maratones internas.
          </p>
        </div>

        {/* Server Error Message */}
        {serverError && (
          <div className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-[13.5px] text-center font-medium">
            {serverError}
          </div>
        )}

        {/* Form Card */}
        <div className="bg-[#101512] border border-[#1f2a24] rounded-[14px] p-[26px]">
          <form onSubmit={handleSubmit}>
            
            {/* Nombre Completo */}
            <div className="mb-[15px]">
              <label className="block text-[11px] font-bold tracking-[0.1em] uppercase text-[#6f847a] mb-[7px]">
                Nombre completo
              </label>
              <input 
                type="text" 
                name="fullName"
                value={formData.fullName}
                onChange={handleChange}
                placeholder="Juan Esteban Restrepo" 
                className="w-full px-[13px] py-[11px] rounded-[9px] bg-[#0c110e] border border-[#253129] text-[#e6efe9] text-[13.5px] outline-none transition-all focus:border-[#2ee88a] focus:ring-4 focus:ring-[#2ee88a]/10 disabled:opacity-50"
                disabled={isLoading}
              />
              {errors.fullName && <p className="mt-2 text-xs text-red-400">{errors.fullName}</p>}
            </div>

            {/* Correo electrónico */}
            <div className="mb-[15px]">
              <label className="block text-[11px] font-bold tracking-[0.1em] uppercase text-[#6f847a] mb-[7px]">
                Correo electrónico
              </label>
              <input 
                type="email" 
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="juan@gmail.com" 
                className="w-full px-[13px] py-[11px] rounded-[9px] bg-[#0c110e] border border-[#253129] text-[#e6efe9] text-[13.5px] outline-none transition-all focus:border-[#2ee88a] focus:ring-4 focus:ring-[#2ee88a]/10 disabled:opacity-50"
                disabled={isLoading}
              />
              {errors.email && <p className="mt-2 text-xs text-red-400">{errors.email}</p>}
            </div>

            {/* Handle */}
            <div className="mb-[15px]">
              <label className="block text-[11px] font-bold tracking-[0.1em] uppercase text-[#6f847a] mb-[7px]">
                Handle
              </label>
              <input 
                type="text" 
                name="username"
                value={formData.username}
                onChange={handleChange}
                placeholder="jrestrepo_cp" 
                className="w-full px-[13px] py-[11px] rounded-[9px] bg-[#0c110e] border border-[#253129] text-[#e6efe9] text-[13.5px] font-mono outline-none transition-all focus:border-[#2ee88a] focus:ring-4 focus:ring-[#2ee88a]/10 disabled:opacity-50"
                disabled={isLoading}
              />
              {errors.username && <p className="mt-2 text-xs text-red-400">{errors.username}</p>}
            </div>

            {/* Contraseña */}
            <div className="mb-[15px]">
              <label className="block text-[11px] font-bold tracking-[0.1em] uppercase text-[#6f847a] mb-[7px]">
                Contraseña
              </label>
              <input 
                type="password" 
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="••••••••" 
                className="w-full px-[13px] py-[11px] rounded-[9px] bg-[#0c110e] border border-[#253129] text-[#e6efe9] text-[13.5px] font-mono outline-none transition-all focus:border-[#2ee88a] focus:ring-4 focus:ring-[#2ee88a]/10 disabled:opacity-50"
                disabled={isLoading}
              />
              {errors.password && <p className="mt-2 text-xs text-red-400">{errors.password}</p>}
            </div>

            <button 
              type="submit"
              disabled={isLoading}
              className="w-full p-[12px] rounded-[9px] bg-[#2ee88a] text-[#06120b] text-[14px] font-bold text-center cursor-pointer transition-all hover:bg-[#5cf0a8] mt-[20px] disabled:opacity-70 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {isLoading && (
                <svg className="animate-spin h-4 w-4 text-[#06120b]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              )}
              {isLoading ? "Procesando..." : "Crear cuenta"}
            </button>
          </form>
          
          <div className="text-center mt-[16px] text-[12.5px] text-[#6f847a]">
            ¿Ya tienes cuenta?{" "}
            <Link href="/login" className="text-[#2ee88a] font-semibold cursor-pointer hover:underline">
              Inicia sesión
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
