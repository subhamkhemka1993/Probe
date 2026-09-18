#!/usr/bin/env bash
# Mechanically enforces the "no mid-block comments" guardrail from CLAUDE.md#Conventions:
# a standalone comment inside a function/property/class body is only allowed as the first
# content of its block or immediately before a declaration — never glued to a line/block
# further into the body after other statements have already run there. See
# scripts/check_no_midblock_comments.py for the exact structural rule and its known limitations.
#
# Usage:
#   check-no-midblock-comments.sh --staged        # newly added comment lines in staged .kt files
#   check-no-midblock-comments.sh --range BASE HEAD  # newly added comment lines between two refs (CI)
#   check-no-midblock-comments.sh --tree          # every tracked .kt file (pre-existing backlog too)
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

CHECKER="scripts/check_no_midblock_comments.py"
MODE="${1:-}"

# Only fails on violations whose comment lines were actually *added* by $1's diff (`--cached`
# for staged, or `A..B` for a ref range) — an existing pre-rule comment elsewhere in an
# otherwise-touched file isn't this commit's/PR's doing. Reads matching .kt paths from stdin.
check_added_lines() {
  local diff_args="$1"
  local added_lines_file
  added_lines_file="$(mktemp)"
  trap 'rm -f "$added_lines_file"' RETURN
  local fail=0
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    [ -f "$f" ] || continue
    : > "$added_lines_file"
    while IFS= read -r hunk; do
      [ -z "$hunk" ] && continue
      start="${hunk%%,*}"
      if [ "$hunk" = "$start" ]; then
        count=1
      else
        count="${hunk#*,}"
      fi
      for ((i = 0; i < count; i++)); do
        echo "$((start + i))" >> "$added_lines_file"
      done
    done < <(git diff $diff_args -U0 -- "$f" | grep -oE '^@@ -[0-9,]+ \+[0-9]+(,[0-9]+)? @@' | sed -E 's/^@@ -[0-9,]+ \+([0-9,]+) @@/\1/')
    [ -s "$added_lines_file" ] || continue

    violations="$(python3 "$CHECKER" "$f" || true)"
    [ -z "$violations" ] && continue

    while IFS= read -r violation; do
      [ -z "$violation" ] && continue
      span="$(echo "$violation" | sed -E "s#^$f:([0-9]+)(-([0-9]+))?:.*#\1 \3#")"
      start="$(echo "$span" | awk '{print $1}')"
      end="$(echo "$span" | awk '{print $2}')"
      [ -z "$end" ] && end="$start"
      if awk -v s="$start" -v e="$end" '$1 >= s && $1 <= e { found=1 } END { exit !found }' "$added_lines_file"; then
        echo "$violation" >&2
        fail=1
      fi
    done <<< "$violations"
  done
  return "$fail"
}

case "$MODE" in
  --staged)
    FILES="$(git diff --cached --name-only --diff-filter=ACM -- '*.kt' || true)"
    [ -z "$FILES" ] && exit 0
    printf '%s\n' "$FILES" | check_added_lines "--cached"
    exit $?
    ;;

  --range)
    BASE="${2:?usage: check-no-midblock-comments.sh --range <base> <head>}"
    HEAD_REF="${3:?usage: check-no-midblock-comments.sh --range <base> <head>}"
    FILES="$(git diff --name-only --diff-filter=ACM "$BASE" "$HEAD_REF" -- '*.kt' || true)"
    [ -z "$FILES" ] && exit 0
    printf '%s\n' "$FILES" | check_added_lines "$BASE $HEAD_REF"
    exit $?
    ;;

  --tree)
    FILES="$(git ls-files -- '*.kt' | grep -v '/build/' || true)"
    [ -z "$FILES" ] && exit 0
    # shellcheck disable=SC2086
    python3 "$CHECKER" $FILES
    exit $?
    ;;

  *)
    echo "usage: $0 --staged | --tree" >&2
    exit 2
    ;;
esac
