#!/usr/bin/env bash
# Enforces the branch naming convention from docs/git-guide.md#branching:
# <type>/<kebab-case-slug>, type one of feat/fix/build/docs/refactor/chore. `main` is exempt
# (existing history commits directly to it; new work should still prefer a branch, but this
# script isn't the place to force that migration).
#
# GIT_BRANCH_OVERRIDE lets CI pass the real source branch name — a checkout of a pull_request
# event lands in detached HEAD, where `git rev-parse --abbrev-ref HEAD` would just report "HEAD"
# and this check would silently no-op.
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

BRANCH="${GIT_BRANCH_OVERRIDE:-$(git rev-parse --abbrev-ref HEAD)}"

if [ "$BRANCH" = "main" ] || [ "$BRANCH" = "HEAD" ]; then
  exit 0
fi

PATTERN='^(feat|fix|build|docs|refactor|chore)/[a-z0-9]+(-[a-z0-9]+)*$'

if ! [[ "$BRANCH" =~ $PATTERN ]]; then
  cat >&2 <<EOF
check-branch-name: branch '$BRANCH' doesn't follow this repo's naming convention:

  <type>/<kebab-case-slug>
  type one of: feat fix build docs refactor chore
  example:     fix/websocket-close-race

See docs/git-guide.md#branching. Rename with:
  git branch -m <new-name>
EOF
  exit 1
fi
