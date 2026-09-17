# Coding guardrails

Formatting, static analysis, and their enforcement points across the four Gradle modules
(`androidApp`, `shared`, `probe-api`, `probe-runtime`). For git-level conventions (branching,
commit messages, hooks), see [git-guide.md](git-guide.md).

## Formatting — Spotless + ktlint

Configured once, in the root `build.gradle.kts`'s `subprojects {}` block, applied to every
module. Rules live in `.editorconfig` at the repo root (`ktlint_code_style = android_studio` is
the single highest-leverage setting — it stops ktlint 1.x from aggressively reformatting things
like single-line function signatures).

```bash
./gradlew spotlessCheck   # verify, no changes (what CI runs)
./gradlew spotlessApply   # auto-fix (what pre-commit runs on staged files)
```

**Adding a new ktlint rule exception:** add it to `.editorconfig` with a one-line comment saying
*why* — e.g. the existing `ktlint_standard_filename = disabled` exception, justified by this
codebase's `Foo.android.kt`/`Foo.ios.kt` KMP platform-source naming convention that never matches
ktlint's single-declaration-per-file rule. A rule disabled without that justification is a rule
someone will re-enable by accident later.

## Static analysis — Android Lint

Each Android-touching module (`androidApp`, `shared`, `probe-api`, `probe-runtime`) declares its
own `lint { }` block with `checkDependencies = true` — this makes Lint recurse into dependency
AARs' bundled `lint.jar`s, which is how Compose's own lint checks (e.g. `ModifierParameter`) get
picked up with no extra dependency.

```bash
./gradlew lint                        # all modules' lint tasks, via the root build
./gradlew :probe-runtime:lint         # one module
```

**This repo's policy is fix, don't baseline — and it's mechanically enforced, not just written
down here.** [`scripts/check-lint-guardrails.sh`](../scripts/check-lint-guardrails.sh) fails (in
`.githooks/pre-commit` and in CI) if:
- any `lint-baseline.xml` file is tracked in git — a previous pass adopted Lint with baselines to
  grandfather pre-existing findings, then went back and fixed every one of them (`InlinedApi`,
  `ModifierParameter`, `ObsoleteSdkInt`, `MonochromeLauncherIcon`, `StaticFieldLeak`,
  `SimilarGradleDependency`, `MissingPermission`, `MissingSuperCall`, `GestureBackNavigation`) and
  deleted the baselines — a new one reappearing means a new finding got grandfathered instead of
  fixed;
- any `build.gradle.kts` disables a Lint issue ID (`disable += "..."`) that isn't listed in
  [`scripts/lint-disabled-checks.conf`](../scripts/lint-disabled-checks.conf) — which is the only
  place a module-wide disable's justification lives, so the allowlist and the reason travel
  together and both are visible in the same diff a reviewer looks at.

If a finding is genuinely out of scope for the change at hand, baseline it *and* open a follow-up
— don't let the guardrail catch it in CI with no explanation; say so in the commit and use
`SKIP_DOCS_CHECK`-style transparency, not a silent bypass.

**Deciding how to suppress a genuine false positive:**
1. Prefer fixing the actual code (rename, restructure, add the missing check) over any
   suppression.
2. If it's a real false positive in Android-only source, use `@SuppressLint("IssueId")` with a
   comment explaining *why* it's safe — see `probe-runtime`'s `Permissions.android.kt` and
   `ProbeNotificationPermissionActivity.kt` for the `InlinedApi` pattern.
3. If the file lives in `commonMain` (no Android annotation available), use a per-module
   `lint.xml` with an `<issue id="..."><ignore path="..."/></issue>` entry, scoped to the
   specific file and issue — see `probe-runtime/lint.xml` (`StaticFieldLeak` for
   `ProbePlatformHolder.kt`) for the pattern and its justifying comment.
4. Only reach for a module-level `disable += "..."` for issues that are structurally noise for
   *this* codebase, like `RestrictedApi` (100% Room-generated `Dao_Impl` classes calling
   Room-internal APIs) or `AndroidGradlePluginVersion`/`GradleDependency` (version-bump
   advisories, not defects) — and add the issue ID to `scripts/lint-disabled-checks.conf` with a
   real reason in the same commit; the guardrail script fails the build otherwise.

## No AI-authorship attribution

Strict, non-negotiable: no commit message, PR/push description, or tracked file's content may
claim or disclose AI-tool authorship — of this project's code, or of the automation that
enforces this rule. Patterns are in
[`scripts/ai-attribution-patterns.conf`](../scripts/ai-attribution-patterns.conf) and checked by
[`scripts/check-no-ai-attribution.sh`](../scripts/check-no-ai-attribution.sh), run:
- on every commit message (`.githooks/commit-msg`),
- on every staged file's added lines (`.githooks/pre-commit`),
- on every commit about to be pushed and the full tree (`.githooks/pre-push` — this re-checks
  even a commit made with `--no-verify`, so skipping the earlier hooks doesn't get it through the
  last local gate),
- and again server-side (`ai-attribution` CI job), which is the one gate a local bypass can't
  reach.

Unlike this repo's other checks, there is **no sanctioned bypass env var** for this one — the
only way past it locally is git's own `--no-verify`, and CI still catches that. `CLAUDE.md` and
`.claude/**/SKILL.md` are exempted from the file-content scan, since they legitimately name the
coding agent they configure — that's tool configuration, not an authorship claim, and the
patterns file's own header explains why.

## Branch naming

[`scripts/check-branch-name.sh`](../scripts/check-branch-name.sh) enforces the
`<type>/<kebab-case-slug>` convention from [git-guide.md#branching](git-guide.md#branching) — run
by `.githooks/pre-commit` locally and by the `guardrails` CI job on pull requests (skipped for
direct pushes to `main`, which are exempt).

## CI

`.github/workflows/ci.yml` runs a `guardrails` job (`check-lint-guardrails.sh`,
`check-branch-name.sh`), `spotlessCheck`, `lint`, tests, an Android debug assemble, and the
docs-freshness check (see [git-guide.md#keeping-docs-in-sync](git-guide.md#keeping-docs-in-sync))
on every push to `main` and every PR. Locally, `.githooks/pre-commit` and `.githooks/pre-push`
catch the fast subset of this before it ever reaches CI — see [git-guide.md#hooks](git-guide.md#hooks).
