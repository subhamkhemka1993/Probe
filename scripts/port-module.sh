#!/usr/bin/env bash
# Mechanically ports a KMP module from zebpay_multiplatform into Probe, renaming
# packages/symbols per docs/superpowers/specs/2026-09-17-probe-migration-design.md.
# Usage: port-module.sh <source-module-dir> <dest-module-dir>
set -euo pipefail

SRC="$1"
DEST="$2"

rm -rf "$DEST"
cp -R "$SRC" "$DEST"

# Stale build output from the source repo's own last build never travels with the port.
rm -rf "$DEST/build"

# zebpay-internal planning docs never travel with the port.
rm -rf "$DEST/docs"

# Relocate every "kotlin/com/zebpay/devtools" package directory to "kotlin/com/dev/probe",
# preserving everything nested underneath (api/, browser/, db/, ui/, etc.) in one move.
find "$DEST" -type d -path '*/kotlin/com/zebpay' | while read -r zebpay_dir; do
  com_dir=$(dirname "$zebpay_dir")
  mkdir -p "$com_dir/dev"
  mv "$zebpay_dir/devtools" "$com_dir/dev/probe"
  rm -rf "$zebpay_dir"
done

# Relocate the Room schema export directory (named after the DB class's FQN), if present.
if [ -d "$DEST/schemas/com.zebpay.devtools.db.ZDebugDatabase" ]; then
  mv "$DEST/schemas/com.zebpay.devtools.db.ZDebugDatabase" "$DEST/schemas/com.dev.probe.db.ProbeDatabase"
fi

# Special-case: ZDebugConfig (zdevtools-internal capture limits) collides with ZToolConfig
# (zdebug-api's public host config) once both are generically renamed to Probe*Config — rename
# this one file before the generic filename pass so it doesn't land on the same name.
find "$DEST" -type f -name 'ZDebugConfig.kt' -exec sh -c 'mv "$1" "$(dirname "$1")/ProbeCaptureLimits.kt"' _ {} \;

# Rename Z-prefixed / zdebug_-prefixed file names (contents are fixed by the sed pass below).
find "$DEST" -type f \( -name 'ZDebug*' -o -name 'ZTool*' -o -name 'zdebug_*' \) | while read -r f; do
  dir=$(dirname "$f")
  base=$(basename "$f")
  newbase=$(printf '%s' "$base" | sed -e 's/^ZDebug/Probe/' -e 's/^ZTool/Probe/' -e 's/^zdebug_/probe_/')
  if [ "$base" != "$newbase" ]; then
    mv "$f" "$dir/$newbase"
  fi
done

# Content substitution across every text file the port touches.
find "$DEST" -type f \( -name '*.kt' -o -name '*.kts' -o -name '*.xml' -o -name '*.md' -o -name '*.js' -o -name '*.html' -o -name '*.css' -o -name '*.json' \) -print0 \
  | xargs -0 sed -i '' \
    -e 's/ZDebugConfig/ProbeCaptureLimits/g' \
    -e 's/com\.zebpay\.devtools/com.dev.probe/g' \
    -e 's/ZDEBUG_/PROBE_/g' \
    -e 's/ZDebug/Probe/g' \
    -e 's/ZTool/Probe/g' \
    -e 's/zDebug/probe/g' \
    -e 's/zTool/probe/g' \
    -e 's/zdebug_/probe_/g' \
    -e 's/zdebugApi/probeApi/g' \
    -e 's/zdebug-api/probe-api/g' \
    -e 's/zdevtools/probe-runtime/g' \
    -e 's/zdebug/probe/g'
