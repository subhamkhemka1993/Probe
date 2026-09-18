# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

`probe` — a Kotlin Multiplatform (Android + iOS) embedded debug shell (network inspector, LAN
browser inspector, dev actions), shipped as two modules: `:probe-api` (always present) and
`:probe-runtime` (heavy implementation, present only where enabled). See
[`docs/guide/README.md`](docs/guide/README.md) for the full framework guide.

## One-time setup

```bash
./scripts/setup-dev-env.sh   # points git at the versioned hooks in .githooks/
```

## Docs

- [`docs/guide/`](docs/guide/README.md) — the framework guide, split by topic (architecture,
  per-platform integration, capability/config reference, security, troubleshooting,
  extensibility, development).
- [`docs/git-guide.md`](docs/git-guide.md) — branching, commit message format, hooks.
- [`docs/coding-guardrails.md`](docs/coding-guardrails.md) — Spotless/ktlint + Android Lint
  conventions, and how to add a suppression correctly.

**Keep `docs/guide/*.md` in sync with source.** A change under `probe-api/` or `probe-runtime/`
that's mapped in [`scripts/docs-sources.conf`](scripts/docs-sources.conf) should update the
paired guide file in the same commit — a git hook and CI job both check this mechanically (see
`docs/git-guide.md#keeping-docs-in-sync`), but the actual prose update is Claude's job, not
theirs. The `probe-docs-sync` skill (`.claude/skills/probe-docs-sync/SKILL.md`) covers this in
more detail — invoke it whenever you touch `:probe-api`/`:probe-runtime` source.

## Common commands

```bash
./gradlew spotlessApply                          # format
./gradlew spotlessCheck lint                     # verify format + static analysis
./gradlew :probe-runtime:testAndroidHostTest :probe-api:testAndroidHostTest
./gradlew :androidApp:assembleDebug
```

## Conventions

- No baselining a new Lint finding as the default response — fix it. Mechanically enforced (not
  just documented) by `scripts/check-lint-guardrails.sh`, which fails on a tracked
  `lint-baseline.xml` or an unlisted `disable += "..."` — see `docs/coding-guardrails.md`.
- Conventional Commits, enforced by `.githooks/commit-msg` — see `docs/git-guide.md`.
- Feature branches follow `<type>/<slug>`, enforced by `scripts/check-branch-name.sh` — see
  `docs/git-guide.md#branching`.
- **Never disclose or claim AI-tool authorship** in a commit message, PR/push description, or any
  tracked file — no attribution trailers, no "Generated with ..." banners, for this tool or any
  other. This is strict and non-negotiable: `scripts/check-no-ai-attribution.sh` enforces it in
  every hook and in CI, with no sanctioned bypass — see `docs/coding-guardrails.md`.
- Most new capability code belongs in `commonMain` with `expect`/`actual` only for the genuinely
  platform-specific edge, to avoid widening the iOS/Android feature gap further than the
  platforms themselves force (see `docs/guide/development.md`).
- **No inline comments marking a mid-function correction.** Comments belong at class-level,
  variable/property-level, and function/method-level (including test functions) to explain
  non-obvious intent or invariants — never scattered inside a function/test/class body to flag or
  narrate a line or block that was just fixed/changed. This is non-negotiable: it accumulates into
  noise that pollutes the function over successive edits.
