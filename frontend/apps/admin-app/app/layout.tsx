import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'Goslint Judge — Panel de Administración',
  description: 'Panel de administración para gestionar problemas, maratones y usuarios del sistema Goslint Judge.',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="es" className="dark">
      <body className="min-h-screen bg-background text-foreground antialiased">
        {children}
      </body>
    </html>
  )
}
