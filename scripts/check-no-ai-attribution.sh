#!/usr/bin/env bash
# Strict, non-negotiable guardrail: no AI-authorship attribution anywhere in this repo — not in
# a commit message, a PR/push description, or a tracked file's content — regardless of which AI
# tool. Patterns live in scripts/ai-attribution-patterns.conf. This is about attribution
# (claims of AI authorship), not the tool name in general — see that file's header for why
# CLAUDE.md / .claude/**/SKILL.md are exempt.
#
# Usage:
#   check-no-ai-attribution.sh --message <file>   # a commit message file, or a PR-description file
#   check-no-ai-attribution.sh --staged           # added lines in staged files (pre-commit)
#   check-no-ai-attribution.sh --tree             # every tracked file's current content (pre-push/CI)
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

PATTERNS_FILE="scripts/ai-attribution-patterns.conf"
PATTERNS_TMP="$(mktemp)"
trap 'rm -f "$PATTERNS_TMP"' EXIT
grep -v '^[[:space:]]*#' "$PATTERNS_FILE" | grep -v '^[[:space:]]*$' > "$PATTERNS_TMP"

is_exempt_path() {
  case "$1" in
    CLAUDE.md|.claude/*|scripts/ai-attribution-patterns.conf|scripts/check-no-ai-attribution.sh) return 0 ;;
    *) return 1 ;;
  esac
}

# Reads candidate text on stdin. On a match, prints the offending line(s) prefixed by $1 and
# returns 1; prints nothing and returns 0 otherwise. `-a` treats binary input as text (so a
# binary asset doesn't abort the scan or print "binary file matches"), `-n` gives line numbers.
check_stdin() {
  local label="$1" found
  found="$(grep -anEif "$PATTERNS_TMP" || true)"
  if [ -n "$found" ]; then
    printf 'check-no-ai-attribution: AI-attribution text found in %s:\n' "$label"
    printf '%s\n' "$found" | sed 's/^/  /'
    return 1
  fi
  return 0
}

FAIL=0
MODE="${1:-}"

case "$MODE" in
  --message)
    FILE="${2:?usage: check-no-ai-attribution.sh --message <file>}"
    if ! OUT="$(check_stdin "commit/PR message" < "$FILE")"; then
      echo "$OUT" >&2
      FAIL=1
    fi
    ;;

  --staged)
    while IFS= read -r f; do
      [ -z "$f" ] && continue
      is_exempt_path "$f" && continue
      ADDED="$(git diff --cached -U0 -- "$f" | grep '^+' | grep -v '^+++' || true)"
      [ -z "$ADDED" ] && continue
      if ! OUT="$(printf '%s\n' "$ADDED" | check_stdin "$f")"; then
        echo "$OUT" >&2
        FAIL=1
      fi
    done < <(git diff --cached --name-only --diff-filter=ACM)
    ;;

  --tree)
    while IFS= read -r f; do
      [ -z "$f" ] && continue
      is_exempt_path "$f" && continue
      [ -f "$f" ] || continue
      if ! OUT="$(check_stdin "$f" < "$f")"; then
        echo "$OUT" >&2
        FAIL=1
      fi
    done < <(git ls-files)
    ;;

  *)
    echo "usage: $0 --message <file> | --staged | --tree" >&2
    exit 2
    ;;
esac

exit "$FAIL"
