import type { Metadata, Viewport } from "next"
import { Geist, Geist_Mono } from "next/font/google"
import "./globals.css"
import "tw-animate-css"

const geistSans = Geist({
  subsets: ["latin"],
  variable: "--font-geist-sans",
})

const geistMono = Geist_Mono({
  subsets: ["latin"],
  variable: "--font-geist-mono",
})

export const metadata: Metadata = {
  title: "Goslint Judge | Plataforma de Programación Competitiva",
  description:
    "Participa en maratones de programación, resuelve problemas desafiantes y recibe retroalimentación con IA para mejorar tus habilidades de algoritmos.",
  generator: "Goslint Judge",
  keywords: [
    "programación competitiva",
    "maratones de código",
    "juez de programación",
    "algoritmos",
    "ICPC",
  ],
}

export const viewport: Viewport = {
  themeColor: "#00ff88",
  width: "device-width",
  initialScale: 1,
}

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es" className="dark bg-background" suppressHydrationWarning>
      <body
        className={`${geistSans.variable} ${geistMono.variable} font-sans antialiased min-h-screen`}
      >
        {children}
      </body>
    </html>
  )
}
