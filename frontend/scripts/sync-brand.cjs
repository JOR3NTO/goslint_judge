/**
 * Copia los archivos de marca de frontend/brand/ a apps/<app>/public/brand/.
 *
 * Uso (desde frontend/):   pnpm brand:sync
 *
 * frontend/brand/ es la fuente única; no editar las copias de public/brand a mano.
 * Sin dependencias: solo módulos nativos de Node.
 */
const fs = require("node:fs")
const path = require("node:path")

const ROOT = path.join(__dirname, "..")
const SOURCE_DIR = path.join(ROOT, "brand")
const TARGET_APPS = ["student-app", "landing-app"]
const FILES = ["logo.png", "logo-dark.png", "logo-mark.png", "logo-mark-dark.png", "og-image.png"]

let copied = 0
for (const app of TARGET_APPS) {
  const targetDir = path.join(ROOT, "apps", app, "public", "brand")
  fs.mkdirSync(targetDir, { recursive: true })

  for (const file of FILES) {
    const source = path.join(SOURCE_DIR, file)
    if (!fs.existsSync(source)) {
      console.error(`Falta el archivo de marca: ${path.relative(ROOT, source)}`)
      process.exitCode = 1
      continue
    }
    fs.copyFileSync(source, path.join(targetDir, file))
    copied++
  }
  console.log(`${app}: ${FILES.length} archivos -> ${path.relative(ROOT, targetDir)}`)
}

if (process.exitCode) {
  console.error("Sincronización incompleta.")
} else {
  console.log(`Listo: ${copied} copias de marca.`)
}
