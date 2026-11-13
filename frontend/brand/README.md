# Marca de la plataforma (fuente única)

Esta carpeta es la **fuente única** del logo de Goslint Judge para el monorepo `frontend/`. Los archivos de aquí se copian a `apps/student-app/public/brand/` y `apps/landing-app/public/brand/` con `pnpm brand:sync` (desde `frontend/`). No editar las copias de `public/brand/` a mano.

## Archivos actuales

Todos son PNG rasterizados con fondo transparente.

| Archivo | Tamaño | Qué es | Uso en el código |
|---|---|---|---|
| `logo.png` | 2919 x 890 | Logo horizontal: símbolo + texto GOSLINT (ratio ~3.28:1), para fondo claro. | Navbar y footer de `student-app` y `landing-app` (variante `full`). |
| `logo-dark.png` | 2919 x 890 | Logo horizontal para fondo oscuro. | Ninguno por ahora (la app solo tiene tema claro). |
| `logo-mark.png` | 1024 x 1024 | Solo el símbolo (cuadrado). | Login y registro de `student-app` (variante `mark`). |
| `logo-mark-dark.png` | 1024 x 1024 | Símbolo para fondo oscuro. | Ninguno por ahora. |
| `og-image.png` | 1200 x 630 | Imagen para previsualizar enlaces (Open Graph / Twitter). | `metadata.openGraph` y `metadata.twitter` de `student-app` y `landing-app`. |
| `favicon-512.png` | 512 x 512 | Origen del icono de la app. | Copiado como `src/app/icon.png` en `student-app`, `landing-app` y `admin-app`. |
| `apple-icon.png` | 180 x 180 | Icono para iOS. | Copiado como `src/app/apple-icon.png` en las tres apps. |
| `favicon-32.png` | 32 x 32 | Icono pequeño de pestaña. | Sin uso directo (Next usa `favicon.ico` y `icon.png`). |
| `favicon.ico` | multitamaño | Icono de pestaña. | Copiado como `src/app/favicon.ico` en las tres apps. |
| `logo-vertical.png` | 768 x 1072 | Variante vertical (símbolo encima del texto). | Sin uso por ahora. |

## Sincronización

```bash
cd frontend
pnpm brand:sync   # node scripts/sync-brand.cjs
```

El script copia `logo.png`, `logo-dark.png`, `logo-mark.png`, `logo-mark-dark.png` y `og-image.png` a `apps/student-app/public/brand/` y `apps/landing-app/public/brand/`. Los iconos de `src/app/` (`icon.png`, `apple-icon.png`, `favicon.ico`) se copian a mano desde esta carpeta si cambian.

## Uso en código

Componente `Logo` (`shared/ui/logo.tsx`, en cada app):

```tsx
<Logo variant="full" className="h-8 w-auto" />  // logo.png, horizontal
<Logo variant="mark" className="h-12 w-12" />   // logo-mark.png, cuadrado
```

El alto lo controla `className`. El componente usa `next/image` con las dimensiones reales de cada PNG.

## Reemplazar por el SVG vectorial

Si el equipo dispone del logo en SVG, conviene usarlo por nitidez. Para hacerlo:

1. Dejar el SVG con el mismo nombre base en esta carpeta (`logo.svg`, `logo-mark.svg`).
2. Cambiar la extensión en `LOGOS` de `shared/ui/logo.tsx` (en ambas apps) y en `FILES` de `scripts/sync-brand.cjs`.
3. Ejecutar `pnpm brand:sync`.

El SVG debe tener el texto convertido a trazos (sin depender de una fuente instalada) y fondo transparente.

## Recomendaciones

- Dejar un margen de al menos el 10 % del tamaño del símbolo alrededor del logo.
- El símbolo debe leerse a 32 px (favicon y footer): evitar detalles finos.
- Mantener el fondo transparente en los PNG (salvo `og-image.png`, que es una imagen completa).
