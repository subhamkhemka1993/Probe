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

**This repo's policy is fix, don't baseline.** A previous pass adopted Lint with baseline files
to grandfather pre-existing findings, then went back and fixed every one of them (`InlinedApi`,
`ModifierParameter`, `ObsoleteSdkInt`, `MonochromeLauncherIcon`, `StaticFieldLeak`,
`SimilarGradleDependency`, `MissingPermission`, `MissingSuperCall`, `GestureBackNavigation`) and
deleted the baseline files — all four modules currently report zero Lint issues. Keep it that
way: a new finding should be fixed, not baselined, unless fixing it is genuinely out of scope for
the change at hand (in which case baseline it *and* open a follow-up, don't let it sit silently).

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
4. Never disable a whole issue ID at the module level (`disable += "..."`) for a single false
   positive — that's for issues that are structurally noise for *this* codebase, like
   `RestrictedApi` (100% Room-generated `Dao_Impl` classes calling Room-internal APIs) or
   `AndroidGradlePluginVersion`/`GradleDependency` (version-bump advisories, not defects — a
   deliberate, separately-verified decision, not something Lint should nag about on every run).

## CI

`.github/workflows/ci.yml` runs `spotlessCheck`, `lint`, tests, an Android debug assemble, and the
docs-freshness check (see [git-guide.md#keeping-docs-in-sync](git-guide.md#keeping-docs-in-sync))
on every push to `main` and every PR. Locally, `.githooks/pre-commit` and `.githooks/pre-push`
catch the fast subset of this before it ever reaches CI — see [git-guide.md#hooks](git-guide.md#hooks).
