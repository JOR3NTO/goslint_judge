# Marca de la plataforma (fuente única)

Esta carpeta es la **fuente única de verdad** del logo de la plataforma para las tres apps del monorepo `frontend/` (`student-app`, `landing-app` y `admin-app`). Aquí se deja el logo original; después se copia a `apps/*/public/brand/` para que las apps lo sirvan.

Hoy el logo es un cuadrito redondeado con el icono `</>` de lucide y el texto "Goslint" (negro) + "Judge" (verde). Se reemplazará por el logo propio.

## Archivos que hay que dejar aquí

Los nombres deben ser **exactos** (minúsculas, con guiones, sin espacios).

| Archivo | Obligatorio | Qué es | Formato preferido |
|---|---|---|---|
| `logo.svg` | Sí | Logotipo horizontal completo: símbolo + palabra, a color, pensado para **fondo claro**. | SVG. Si solo hay PNG: `logo.png`, mínimo 1000 px de ancho, fondo transparente. |
| `logo-mark.svg` | Sí | Solo el símbolo (isotipo), cuadrado. Para el icono pequeño, favicon y avatares. | SVG. Alternativa: `logo-mark.png` de 512 x 512 px, fondo transparente. |
| `logo-dark.svg` | Opcional | Versión del logotipo completo para **fondo oscuro**, por si vuelve a existir un tema oscuro. | SVG (o `logo-dark.png`, ≥ 1000 px de ancho). |
| `favicon.ico` o `favicon.png` | Opcional | Icono de la pestaña del navegador. | `favicon.png` de 32 o 48 px; `favicon.ico` si se quiere multitamaño. |
| `og-image.png` | Opcional | Imagen para previsualizar enlaces (WhatsApp, LinkedIn, etc.). | PNG de 1200 x 630 px. |

Si no hay favicon ni imagen OG, no pasa nada: se puede generar más adelante a partir de `logo-mark.svg`.

## Uso real de cada archivo (inventario del código actual)

| Archivo | Dónde se usaría | Tamaño de uso real hoy (referencia) |
|---|---|---|
| `logo.svg` | Navbar de `student-app` (`shared/ui/navbar.tsx`) y `landing-app` (`shared/ui/navbar.tsx`); pie de página de `student-app` y `landing-app` (`shared/ui/footer.tsx`) | Navbar: alto ~36 px (tile h-9) + texto text-xl; ancho total aprox. 170 px. Footer: alto ~32 px en `student-app` y ~36 px en `landing-app` |
| `logo-mark.svg` | Tarjetas de login y registro (`features/auth/components/login-page.tsx` y `register-page.tsx`); navbar y footer (tiles); favicon; avatares | Login/registro: 48 x 48 px (h-12 w-12). Navbar: 36 x 36 px. Footer: 32 x 32 (student) o 36 x 36 (landing). Favicon: 32 px |
| `logo-dark.svg` | Ninguno hoy. Solo si se reactiva el tema oscuro (`admin-app` usa `class="dark"` en su layout) | Igual que `logo.svg` |
| `favicon.png` / `favicon.ico` | Ninguna app lo tiene configurado (no hay `icons` en `metadata`) | 32 px (pestaña) o 48 px |
| `og-image.png` | Ninguna app tiene `openGraph` en `metadata` | 1200 x 630 px |

## Recomendaciones

- **Márgenes de aire:** deja alrededor del logo un margen de al menos el 10 % del tamaño del símbolo. Así no se corta en los tiles redondeados.
- **SVG sin fuentes:** convierte el texto a trazos (vectores), tipo "Convertir a contorno / Outline text" en Illustrator o Inkscape. Si el SVG depende de una fuente instalada, se verá distinto en otros equipos.
- **Fondo transparente** tanto en SVG como en PNG. Sin fondo blanco incorporado.
- **Relación de aspecto del logotipo horizontal:** el logo actual (tile + texto) ronda 4,7:1 (ancho:alto). Para el nuevo, procura algo entre 3:1 y 5:1 para que quepa en la navbar sin deformarse. Usa `viewBox` correcto y no fijes ancho y alto a la vez con valores que no sean proporcionales.
- **El símbolo debe leerse a 32 px** (favicon y footer): evita detalles finos o textos dentro del símbolo.
- **Nombres:** respeta exactamente los nombres de la tabla; el código los referenciará por ruta.

---

Cuando coloques los archivos aquí, avisa para conectarlos en las tres apps (copia a `apps/*/public/brand/`, componente `Logo` y metadatos).
