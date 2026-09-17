---
name: probe-docs-sync
description: Use whenever editing probe-api or probe-runtime source (or their build.gradle.kts files) — checks whether the change affects anything documented in docs/guide/*.md and updates the paired guide file in the same turn, instead of letting the guide drift stale.
---

# probe-docs-sync

`docs/guide/` is probe's framework guide, split into one file per topic (architecture,
per-platform integration, capability/configuration reference, security, troubleshooting,
extensibility, development). Each guide file is paired with the source paths it documents in
[`../../scripts/docs-sources.conf`](../../scripts/docs-sources.conf).

A mechanical guardrail already exists — `.githooks/pre-commit` runs
`scripts/check-docs-freshness.sh --staged` and blocks a commit that touches a mapped source path
without touching the paired guide file (see `docs/git-guide.md#keeping-docs-in-sync`). That catches
the *fact* that something might be stale; it can't write the correct prose. That's this skill's job.

## When this applies

You're editing (or about to edit) something under `probe-api/` or `probe-runtime/` — source files,
`build.gradle.kts`, `AndroidManifest.xml`, or `lint.xml` — and the change is more than a pure
internal rename/refactor with no externally-visible effect (public API shape, integration steps,
configuration surface, platform behavior differences, security posture, or troubleshooting
symptoms).

## What to do

1. Before editing, skim [`../../scripts/docs-sources.conf`](../../scripts/docs-sources.conf) for
   any `DOC:`/`SRC:` block whose `SRC:` path matches (or is a parent of) the file you're about to
   change.
2. Make your source change as normal.
3. If a matching guide file exists, open it and update the prose so it still accurately describes
   the new behavior — don't just append a note; edit the relevant section so the doc reads as if
   it were written fresh against the new code. Keep the doc's existing tone: concrete, sourced from
   the actual code (cite real file paths, real type/function names), no marketing language, and
   explicit about platform differences and limitations rather than glossing over them.
4. If your change introduces a new public API, capability, or config field with no existing guide
   section, add one (in the most relevant existing file, or a new file linked from
   `docs/guide/README.md`'s index table) and add a matching `DOC:`/`SRC:` block to
   `scripts/docs-sources.conf` so future changes to it are tracked too.
5. If you determine no guide update is actually needed (pure internal change), say so explicitly
   in your commit message body and, if committing yourself, pass `SKIP_DOCS_CHECK=1` rather than
   silently working around the hook.

## What NOT to do

- Don't regenerate a whole guide file from scratch for a small change — edit the specific section
  affected.
- Don't add prose that duplicates what's already accurately said elsewhere in the guide; link to
  the existing file/section instead (every file already links back to `docs/guide/README.md`).
- Don't relax or remove the `docs-sources.conf` mapping just to make a commit pass — fix the doc,
  or justify the bypass, not the mapping (unless the mapping itself is factually wrong, e.g. it
  points at a path that moved).
