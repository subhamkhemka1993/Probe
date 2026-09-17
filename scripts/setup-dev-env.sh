#!/usr/bin/env bash
# One-time (idempotent) dev environment setup: points git at the versioned hooks in
# .githooks/ (git never reads .git/hooks from version control, so this has to be a script
# every contributor runs once — see docs/git-guide.md#hooks).
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

chmod +x .githooks/* scripts/*.sh
git config core.hooksPath .githooks

echo "Dev environment ready:"
echo "  - git hooks now run from .githooks/ (pre-commit, commit-msg, pre-push)"
echo "  - see docs/git-guide.md and docs/coding-guardrails.md for what they enforce and why"
