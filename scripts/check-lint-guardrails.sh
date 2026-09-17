#!/usr/bin/env bash
# Mechanically enforces the "fix, don't baseline" Lint policy from
# docs/coding-guardrails.md#static-analysis--android-lint:
#   1. No lint-baseline.xml may be tracked in git (grandfathering findings back in is banned).
#   2. Every module-wide `disable += "IssueId"` in a build.gradle.kts must be in the allowlist
#      at scripts/lint-disabled-checks.conf, with a stated reason — no silent blanket disables.
#
# Usage: scripts/check-lint-guardrails.sh
# Exit 0 = clean. Exit 1 = violation found, printed to stderr.
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

FAIL=0

BASELINES="$(git ls-files -- '*lint-baseline.xml' || true)"
if [ -n "$BASELINES" ]; then
  echo "check-lint-guardrails: lint-baseline.xml file(s) are tracked in git — findings must be" >&2
  echo "fixed, not baselined (see docs/coding-guardrails.md):" >&2
  printf '%s\n' "$BASELINES" | sed 's/^/  - /' >&2
  FAIL=1
fi

ALLOWLIST="scripts/lint-disabled-checks.conf"
allowed_ids() {
  grep -v '^\s*#' "$ALLOWLIST" | grep -v '^\s*$' | sed 's/:.*//'
}

GRADLE_FILES="$(git ls-files -- '*build.gradle.kts' || true)"
while IFS= read -r gradle_file; do
  [ -z "$gradle_file" ] && continue
  while IFS= read -r issue_id; do
    [ -z "$issue_id" ] && continue
    if ! allowed_ids | grep -qxF "$issue_id"; then
      echo "check-lint-guardrails: $gradle_file disables Lint issue '$issue_id', which is not" >&2
      echo "in the allowlist ($ALLOWLIST). Either fix the underlying findings instead of" >&2
      echo "disabling the check, or add '$issue_id: <reason>' to the allowlist with a real" >&2
      echo "justification and get it reviewed." >&2
      FAIL=1
    fi
  done < <(grep -oE 'disable\s*\+=\s*"[A-Za-z0-9]+"' "$gradle_file" | sed -E 's/.*"([A-Za-z0-9]+)"/\1/')
done <<< "$GRADLE_FILES"

exit "$FAIL"
