#!/usr/bin/env bash
# Flags a change to a documented source path that isn't paired with a change to the guide
# file that documents it (mapping: scripts/docs-sources.conf). This is the mechanical half of
# "keep docs/guide in sync" — see docs/git-guide.md#keeping-docs-in-sync and the probe-docs-sync
# Claude Code skill (.claude/skills/probe-docs-sync/SKILL.md) for the editorial half.
#
# Usage:
#   scripts/check-docs-freshness.sh                # hook mode: diff working tree + index vs HEAD
#   scripts/check-docs-freshness.sh --staged       # diff only the git index vs HEAD (pre-commit)
#   scripts/check-docs-freshness.sh <base> <head>  # CI mode: diff base..head (e.g. origin/main HEAD)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONF="$REPO_ROOT/scripts/docs-sources.conf"
cd "$REPO_ROOT"

if [ "${1:-}" = "--staged" ]; then
  CHANGED="$(git diff --cached --name-only)"
elif [ $# -eq 2 ]; then
  CHANGED="$(git diff --name-only "$1" "$2")"
else
  CHANGED="$(git diff --name-only HEAD; git diff --cached --name-only)"
fi
CHANGED="$(printf '%s\n' "$CHANGED" | sort -u | sed '/^$/d')"

if [ -z "$CHANGED" ]; then
  exit 0
fi

path_in_list() {
  # $1 = changed file, $2 = newline-separated list of changed files
  printf '%s\n' "$2" | grep -qxF "$1"
}

STALE=""
DOC=""
SRCS=""

flush_block() {
  if [ -n "$DOC" ] && [ -n "$SRCS" ]; then
    DOC_CHANGED=0
    if path_in_list "$DOC" "$CHANGED"; then
      DOC_CHANGED=1
    fi
    SRC_CHANGED=0
    while IFS= read -r src; do
      [ -z "$src" ] && continue
      while IFS= read -r changed_file; do
        [ -z "$changed_file" ] && continue
        case "$changed_file" in
          "$src"|"$src"/*) SRC_CHANGED=1 ;;
        esac
      done <<EOF_INNER
$CHANGED
EOF_INNER
    done <<EOF_OUTER
$SRCS
EOF_OUTER
    if [ "$SRC_CHANGED" -eq 1 ] && [ "$DOC_CHANGED" -eq 0 ]; then
      STALE="$STALE
  - $DOC (sources changed: $(printf '%s' "$SRCS" | tr '\n' ' '))"
    fi
  fi
  DOC=""
  SRCS=""
}

while IFS= read -r line || [ -n "$line" ]; do
  case "$line" in
    DOC:*) flush_block; DOC="${line#DOC:}" ;;
    SRC:*) SRCS="$SRCS
${line#SRC:}" ;;
    "") flush_block ;;
    "#"*) ;;
  esac
done < "$CONF"
flush_block

if [ -n "$STALE" ]; then
  echo "docs/guide may be stale — the following guide files document source paths that changed" >&2
  echo "in this diff, but the guide file itself did not change:" >&2
  echo "$STALE" >&2
  echo "" >&2
  echo "Either update the listed guide file(s) in this commit, or if the change genuinely doesn't" >&2
  echo "affect documented behavior, re-run with SKIP_DOCS_CHECK=1 to bypass (git commit/push only)." >&2
  exit 1
fi

exit 0
