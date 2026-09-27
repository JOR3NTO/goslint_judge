import { Code2, Github, Twitter, Mail } from "lucide-react"
import { env } from "@/core/config/env"

export function Footer() {
  return (
    <footer className="border-t border-border bg-card/50">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand */}
          <div className="space-y-4">
            <a href="/" className="flex items-center gap-2">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 border border-primary/30">
                <Code2 className="h-5 w-5 text-primary" />
              </div>
              <span className="text-xl font-bold text-foreground">
                Goslint<span className="text-primary">Judge</span>
              </span>
            </a>
            <p className="text-sm text-muted-foreground">
              La plataforma de juez de programación competitiva con retroalimentación de IA.
            </p>
          </div>

          {/* Plataforma — links a student-app */}
          <div>
            <h3 className="font-semibold text-foreground mb-4">Plataforma</h3>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>
                <a href={`${env.APP_URL}/login`} className="hover:text-primary transition-colors">
                  Maratones Activas
                </a>
              </li>
              <li>
                <a href={`${env.APP_URL}/login`} className="hover:text-primary transition-colors">
                  Maratones Anteriores
                </a>
              </li>
              <li>
                <a href={`${env.APP_URL}/login`} className="hover:text-primary transition-colors">
                  AI Feedback
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="font-semibold text-foreground mb-4">Recursos</h3>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Documentación
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Problemas de Práctica
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Ranking Global
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="font-semibold text-foreground mb-4">Legal</h3>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Términos de Servicio
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Política de Privacidad
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Código de Conducta
                </a>
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-12 pt-8 border-t border-border flex flex-col sm:flex-row items-center justify-between gap-4">
          <p className="text-sm text-muted-foreground">
            © 2025 Goslint Judge. Todos los derechos reservados.
          </p>
          <div className="flex items-center gap-4">
            <a href="#" className="text-muted-foreground hover:text-primary transition-colors">
              <Github className="h-5 w-5" />
            </a>
            <a href="#" className="text-muted-foreground hover:text-primary transition-colors">
              <Twitter className="h-5 w-5" />
            </a>
            <a href="#" className="text-muted-foreground hover:text-primary transition-colors">
              <Mail className="h-5 w-5" />
            </a>
          </div>
        </div>
      </div>
    </footer>
  )
}
