# Git guide

This repo is currently local-only (no `origin` remote configured) and history so far lives
directly on `main`. The conventions below apply from here on, including to work that stays local.

## One-time setup

```bash
./scripts/setup-dev-env.sh
```

This points git at the versioned hooks in `.githooks/` (`git config core.hooksPath .githooks`) —
git never reads hooks out of version control on its own, so every clone needs to run this once.

## Branching

Work in a feature branch, not directly on `main`:

- `feat/<slug>` — new capability (e.g. `feat/browser-inspector-auth`)
- `fix/<slug>` — bug fix
- `build/<slug>` — build/tooling/dependency changes
- `docs/<slug>` — documentation-only changes
- `refactor/<slug>` — behavior-preserving restructuring
- `chore/<slug>` — everything else (deps bump, cleanup)

Keep `<slug>` short and kebab-case. Rebase onto `main` before merging rather than merging `main`
into your branch, to keep history linear; merge (don't squash) into `main` once green so
individual commits — which should already be small and self-contained — stay intact.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/), enforced by `.githooks/commit-msg`:

```
<type>(<optional scope>): <subject>

<optional body>
```

- `type` ∈ `feat fix build chore refactor docs test perf ci style revert`
- `scope` is usually the module touched (`probe-api`, `probe-runtime`, `androidApp`, `shared`)
- Subject: imperative, lowercase after the colon, no trailing period
- Match the existing history's style, e.g.:
  - `fix(probe-runtime): guard ProbeActivity against a disabled runtime`
  - `build: replace detekt/raw ktlint with Spotless+ktlint and Android Lint`
  - `docs: split the framework guide into docs/guide/*.md`

## Hooks

Installed via `scripts/setup-dev-env.sh`, sourced from `.githooks/` (kept in version control,
unlike `.git/hooks/`):

| Hook | What it does | Bypass |
|---|---|---|
| `pre-commit` | Runs `spotlessApply` on staged `.kt`/`.kts` files and re-stages them; runs the docs-freshness check against staged changes | `SKIP_DOCS_CHECK=1 git commit`, or `git commit --no-verify` for the formatting step too |
| `commit-msg` | Enforces the Conventional Commits format above | `git commit --no-verify` |
| `pre-push` | Runs `spotlessCheck` (fast; full `lint`/tests run in CI, not here) | `SKIP_PREPUSH=1 git push` |

Bypassing a hook should be the exception, not the default — if a hook is wrong for a real
situation, fix the hook (it's a versioned script anyone can edit and send through review), don't
just get in the habit of skipping it.

## Keeping docs in sync

`docs/guide/*.md` is a split-out framework guide, each file mapped to the source paths it
documents in [`../scripts/docs-sources.conf`](../scripts/docs-sources.conf). `pre-commit` runs
`scripts/check-docs-freshness.sh --staged`, which blocks a commit that touches a mapped source
path without touching the paired guide file, and CI (`.github/workflows/ci.yml`, `docs-freshness`
job) re-runs the same check over the full PR diff so this isn't only a local, skippable gate.

When it fires:
1. Open the guide file it names and update the prose to match your change.
2. If the change genuinely has no documented-behavior impact (e.g. an internal rename with no
   externally visible effect), re-run the failing command with `SKIP_DOCS_CHECK=1` and say why in
   the commit body — don't silently bypass it.
3. If a source path's mapping in `scripts/docs-sources.conf` is wrong or missing, fix the mapping
   itself — it's meant to track reality, not the other way around.

When Claude Code is making the change, the `probe-docs-sync` skill
(`.claude/skills/probe-docs-sync/SKILL.md`) is expected to do step 1 or 2 as part of the same
turn, rather than leaving it for the hook to catch.
