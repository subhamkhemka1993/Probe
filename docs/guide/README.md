# probe — framework guide

probe is a Kotlin Multiplatform (Android + iOS) embedded debug shell: an in-app network
inspector, request/response viewer, LAN browser-based traffic viewer, and a small set of
developer actions (clear app data, permission inspection), all reachable from inside a running
app without a debugger attached. It is built for Compose Multiplatform apps that use Ktor for
networking; no dependency-injection framework is required to integrate it.

The problem it solves: verifying "what did this app actually send/receive over the network" or
"what's in this app's debug preferences right now" on a real device — especially a tester's or a
teammate's device — usually means attaching a proxy, plugging in a cable, or asking someone to
read logs over Slack. probe puts that inspector *inside the app*, gated so it structurally
cannot ship to production, and reachable by a developer or QA tester without any extra tooling
on their machine.

> **Scope of this guide.** This describes probe's own capabilities and integration surface,
> independent of any specific host application. Where an example is drawn from this repository's
> own integration, it is explicitly labeled as such — the API and behavior described are the
> library's, not this host app's.

## Contents

| File | Covers |
|---|---|
| [architecture.md](architecture.md) | Module split, settable-hook pattern, runtime lifecycle diagram |
| [requirements.md](requirements.md) | Platform/toolchain/dependency versions |
| [integration-guide.md](integration-guide.md) | The 5 platform-agnostic integration steps |
| [integration-android.md](integration-android.md) | Android-specific wiring, manifest merge, launcher icon |
| [integration-ios.md](integration-ios.md) | iOS-specific wiring, notification bridge pattern |
| [capability-reference.md](capability-reference.md) | Per-feature reference + iOS/Android feature matrix |
| [configuration-reference.md](configuration-reference.md) | `ProbeConfig` / `ProbeThemeOverride` / `ProbeHostCallbacks` field reference |
| [usage-examples.md](usage-examples.md) | Copy-paste snippets for common integration tasks |
| [security.md](security.md) | Production-safety model, redaction, what probe does *not* protect against |
| [troubleshooting-and-faq.md](troubleshooting-and-faq.md) | Symptom → cause → fix, FAQ, and known usage gaps |
| [extensibility.md](extensibility.md) | Adding a new inspector plugin |
| [development.md](development.md) | Repo layout, build/test commands, where new code should live |
| [publishing.md](publishing.md) | Maven Central setup, release process, `com.vanniktech.maven.publish` config |

## Keeping this guide in sync

Each file above is paired with the source paths it documents in
[`../../scripts/docs-sources.conf`](../../scripts/docs-sources.conf). `scripts/check-docs-freshness.sh`
uses that mapping to flag (and, on a plain local commit, block) a commit that touches a mapped
source path without touching the paired guide file — wired in as the `.githooks/pre-commit` hook
and as a CI job. See [`../git-guide.md`](../git-guide.md#keeping-docs-in-sync) for how to install
the hook and what to do when it fires, and the `probe-docs-sync` Claude Code skill
(`.claude/skills/probe-docs-sync/SKILL.md`) for how Claude Code itself is expected to keep this
guide current while making source changes.
