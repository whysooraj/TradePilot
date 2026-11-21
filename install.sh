#!/usr/bin/env bash
set -euo pipefail

# TradeLab One-Line Installer
# Usage: curl -fsSL https://raw.githubusercontent.com/whysooraj/TradePilot/main/install.sh | bash

REPO_URL="https://github.com/whysooraj/TradePilot.git"
INSTALL_DIR="TradePilot"

printf "\n[1/4] Checking environment prerequisites...\n"

command -v git >/dev/null 2>&1 || {
  printf "Error: git is required but not installed.\n" >&2
  exit 1
}

# Check docker and compose availability
USE_DOCKER=false
if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  if docker compose version >/dev/null 2>&1 || command -v docker-compose >/dev/null 2>&1; then
    USE_DOCKER=true
  fi
fi

printf "\n[2/4] Fetching TradeLab repository...\n"
if [ -d "$INSTALL_DIR/.git" ]; then
  printf "Found existing %s directory, pulling latest changes...\n" "$INSTALL_DIR"
  cd "$INSTALL_DIR"
  git pull origin main
else
  git clone "$REPO_URL" "$INSTALL_DIR"
  cd "$INSTALL_DIR"
fi

if [ "$USE_DOCKER" = true ]; then
  printf "\n[3/4] Launching containers with Docker Compose...\n"
  if docker compose version >/dev/null 2>&1; then
    docker compose up -d --build
  else
    docker-compose up -d --build
  fi

  printf "\n[4/4] TradeLab deployed successfully.\n"
  printf "--------------------------------------------------------\n"
  printf "Frontend Application:   http://localhost:3000\n"
  printf "Backend REST API:       http://localhost:8080/api/v1\n"
  printf "Swagger Documentation:  http://localhost:8080/swagger-ui.html\n"
  printf "--------------------------------------------------------\n"
  printf "To stop services: cd %s && docker compose down\n\n" "$INSTALL_DIR"
  exit 0
fi

# Native setup fallback
printf "\nDocker not found or daemon not running. Attempting native launch...\n"

command -v java >/dev/null 2>&1 || {
  printf "Error: Java 21+ or Docker is required to run TradeLab.\n" >&2
  exit 1
}

command -v npm >/dev/null 2>&1 || {
  printf "Error: Node.js/npm is required for frontend.\n" >&2
  exit 1
}

printf "\n[3/4] Building backend and frontend natively...\n"

# Backend build
cd backend
if command -v mvn >/dev/null 2>&1; then
  mvn clean package -DskipTests
elif command -v ./mvnw >/dev/null 2>&1; then
  ./mvnw clean package -DskipTests
else
  printf "Notice: Maven not found in PATH. Please install Maven or run Docker.\n" >&2
  exit 1
fi
cd ..

# Frontend build
cd frontend
npm ci || npm install
npm run build
cd ..

printf "\n[4/4] Build complete.\n"
printf "--------------------------------------------------------\n"
printf "Run Backend:  cd %s/backend && java -jar target/tradelab-backend-1.0.0-SNAPSHOT.jar\n" "$INSTALL_DIR"
printf "Run Frontend: cd %s/frontend && npm run preview\n" "$INSTALL_DIR"
printf "--------------------------------------------------------\n\n"
