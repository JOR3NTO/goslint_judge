import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },
  allowedDevOrigins: ['192.168.18.51', '127.0.2.2'],
  // Rewrites por prefijo: Next.js aplica la PRIMERA regla que coincide, así que
  // el orden va de lo más específico a lo más general.
  async rewrites() {
    const AUTH_SERVICE = process.env.AUTH_SERVICE_URL ?? 'http://localhost:8081'
    const PROBLEM_SERVICE = process.env.PROBLEM_SERVICE_URL ?? 'http://localhost:8082'
    const SUBMISSION_SERVICE = process.env.SUBMISSION_SERVICE_URL ?? 'http://localhost:8083'
    const CONTEST_SERVICE = process.env.CONTEST_SERVICE_URL ?? 'http://localhost:8086'

    return [
      // contest-service (8086): maratones, equipos, invitaciones y problemas de maratón.
      // "/api/v1/contests/:path*" ya cubre /contests/:id/problems, /teams y /scoreboard.
      { source: '/api/v1/contests/:path*', destination: `${CONTEST_SERVICE}/api/v1/contests/:path*` },
      { source: '/api/v1/contests', destination: `${CONTEST_SERVICE}/api/v1/contests` },
      { source: '/api/v1/contest-problems/:path*', destination: `${CONTEST_SERVICE}/api/v1/contest-problems/:path*` },
      { source: '/api/v1/teams/:path*', destination: `${CONTEST_SERVICE}/api/v1/teams/:path*` },
      { source: '/api/v1/invitations/:path*', destination: `${CONTEST_SERVICE}/api/v1/invitations/:path*` },
      // Responder aclaraciones (POST /api/v1/clarifications/{id}/answer) también vive en contest-service.
      { source: '/api/v1/clarifications/:path*', destination: `${CONTEST_SERVICE}/api/v1/clarifications/:path*` },
      // problem-service (8082)
      { source: '/api/v1/problems/:path*', destination: `${PROBLEM_SERVICE}/api/v1/problems/:path*` },
      // submission-service (8083)
      { source: '/api/v1/submissions/:path*', destination: `${SUBMISSION_SERVICE}/api/v1/submissions/:path*` },
      // Resto de /api/v1 (auth, users, admin) -> auth-service (8081)
      { source: '/api/v1/:path*', destination: `${AUTH_SERVICE}/api/v1/:path*` },
    ]
  },
  turbopack: {
    root: path.resolve(__dirname, '../..'),
  },
}

export default nextConfig
