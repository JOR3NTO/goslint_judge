#!/bin/bash
# Arranca el panel de integración en http://localhost:4200
cd "$(dirname "$0")"
[ -d node_modules ] || pnpm install
node server.js
