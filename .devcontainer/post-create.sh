#!/usr/bin/env bash
set -euo pipefail

echo "=== Azadi Dev Container Setup ==="

# ============================================================
# Install Firestore emulator (gcloud CLI installed by devcontainer feature)
# ============================================================
sudo apt-get update -qq \
    && sudo apt-get install -y -qq --no-install-recommends google-cloud-cli-firestore-emulator \
    && sudo apt-get clean && sudo rm -rf /var/lib/apt/lists/*

# ============================================================
# Fix permissions
# ============================================================
sudo chown vscode:vscode /home/vscode/.m2 /home/vscode/.npm 2>/dev/null || true

# ============================================================
# Claude Code config - symlink ~/.claude.json from mounted dir
# ============================================================
if [ -f ~/.claude/.claude.json ] && [ ! -e ~/.claude.json ]; then
    ln -s ~/.claude/.claude.json ~/.claude.json
fi

# ============================================================
# NPM config
# ============================================================
npm config set cache ~/.npm
npm config set update-notifier false
npm config set fund false
npm config set audit false

# ============================================================
# Git config
# ============================================================
git config --global --add safe.directory /workspace
git config --global init.defaultBranch main
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.ci commit

# ============================================================
# Shell aliases
# ============================================================
cat >> ~/.zshrc << 'ALIASES'

# Claude
alias claude="claude --dangerously-skip-permissions"

# Azadi aliases
# Azadi dev aliases
alias dev='cd /workspace && set -a && source local.env && set +a && FIRESTORE_EMULATOR_HOST=localhost:8081 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev "-Dspring-boot.run.jvmArguments=--add-opens java.base/java.math=ALL-UNNAMED" -P no-checks'
alias dev-frontend="cd /workspace/frontend && npm run dev"
alias fb-emulator="gcloud emulators firestore start --host-port=0.0.0.0:8081 --database-mode=datastore-mode --project=demo-azadi"
alias fb-emulator-reset="kill \$(lsof -ti:8081) 2>/dev/null; sleep 1; fb-emulator"
alias test-unit="cd /workspace && ./mvnw test -P no-checks"
alias test-all="cd /workspace && ./mvnw verify -P no-checks"
alias build-jar="cd /workspace && ./mvnw package -P no-checks,frontend -DskipTests"
alias lint="cd /workspace/frontend && npm run lint && npm run format:check"

# Docker
alias dc='docker compose'
alias dcup='docker compose up -d'
alias dcdown='docker compose down'

ALIASES

[ -f ~/.bashrc ] && ! grep -q 'exec zsh' ~/.bashrc && echo '[ -t 1 ] && exec zsh' >> ~/.bashrc

# ============================================================
# Install dependencies in parallel
# ============================================================
echo "Installing dependencies..."

(cd /workspace && ./mvnw dependency:resolve -P no-checks -B > /tmp/maven-install.log 2>&1 && echo "Maven deps: OK" || echo "Maven deps: FAILED") &
(cd /workspace/frontend && npm ci > /tmp/npm-install.log 2>&1 && echo "Frontend deps: OK" || echo "Frontend deps: FAILED") &

wait

echo "=== Setup complete ==="
echo ""
echo "Quick start:"
echo "  fb-emulator      -> Start Firestore emulator (:8081)"
echo "  dev-frontend     -> Start Vite dev server (:5173)"
echo "  dev              -> Start Spring Boot (:8080)"
echo "  test-unit        -> Run unit tests"
echo "  test-all         -> Run all tests (unit + integration)"
echo "  build-jar        -> Build production JAR"
echo ""
echo "Workflow: Open 3 terminals:"
echo "  1. fb-emulator     (Firestore)"
echo "  2. dev-frontend    (Vite HMR on :5173)"
echo "  3. dev             (Spring Boot on :8080)"
echo "  -> Open http://localhost:8080"
echo ""
