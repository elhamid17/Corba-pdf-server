#!/bin/bash
# ═══════════════════════════════════════════
# build.sh — Compile et lance le projet
# ═══════════════════════════════════════════

set -e  # Arrêt immédiat si une commande échoue

echo "═══════════════════════════════════════"
echo "  CORBA PDF Server — Build"
echo "═══════════════════════════════════════"

# Compiler les modules Java
echo ""
echo "▶ Compilation Maven..."
mkdir -p .m2/repository
mvn clean package -DskipTests -Dmaven.repo.local="$(pwd)/.m2/repository" -q
echo "✓ Compilation réussie"

# Lancer Docker Compose
echo ""
echo "▶ Démarrage Docker Compose..."
docker compose up --build -d

echo ""
echo "✓ Tous les services démarrés"
echo ""
echo "  Frontend  → http://localhost"
echo "  API       → http://localhost/api/pdf/ping"
echo "  Health    → http://localhost/health"
echo ""
echo "  Logs : docker compose logs -f"
echo "  Stop : docker compose down"
