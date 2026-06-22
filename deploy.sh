#!/bin/bash
# ═══════════════════════════════════════════
# deploy.sh — Déploiement sur VPS
# ═══════════════════════════════════════════

set -e

echo "═══════════════════════════════════════"
echo "  PDF Suite — Déploiement VPS"
echo "═══════════════════════════════════════"

# Pull dernière version
echo "▶ Pull GitHub..."
git pull origin main

# Rebuild et restart
echo "▶ Rebuild des conteneurs..."
docker compose down
docker compose up --build -d

echo ""
echo "✓ Déploiement terminé"
echo "  Vérification : docker compose ps"