# Smart Resource Multiplier public release checklist

## Smart Resource Multiplier 1.2.3

This patch is limited to the approved production icon, versioned documentation, and reproducible Java 25 toolchain selection. Gameplay, configuration semantics, schema, networking, permissions, persistence, and anti-duplication behavior are unchanged.

## Branding and compatibility gates

- [x] Fabric and Mod Menu expose the public name **Smart Resource Multiplier**.
- [x] Configuration screens, reset prompts, commands, diagnostics, issue forms, and maintained documentation retain the public name.
- [x] The Fabric mod ID remains `smart_resource_drops`.
- [x] `config/smart_resource_drops.json`, schema 3, datapack and network namespaces, saved provenance, Java packages, `/smartdrops`, and `/smartdropsgui` remain unchanged.
- [x] New build artifacts use `smart-resource-multiplier-<version>.jar`.
- [x] Deterministic release packages use the `SmartResourceMultiplier-*` prefix.
- [x] Upgrade documentation tells players to remove the older JAR before installing the new JAR.
- [x] The production icon is the reviewed `512x512` PNG documented in [`BRANDING.md`](BRANDING.md), with SHA-256 `db216ccd6058404de18f797ebb5be87a313899a27c3f1971fdf086b8637dc190`.
- [x] The published `v1.2.2` release and its original icon remain immutable historical evidence.

## Metadata and documentation gates

- [x] `mod_version` is `1.2.3`.
- [x] The fully verified release commit sets `release_ready=true`.
- [x] Fabric `contact.homepage` remains the official CurseForge project.
- [x] Fabric `contact.issues` remains GitHub Issues.
- [x] Fabric `contact.sources` remains the public GitHub repository.
- [x] README, changelog, implementation log, build status, release notes, validators, and package tests document the icon and toolchain patch.
- [x] Historical release and verification records preserve the public names and artifact identities that were true for those releases.

## Automated gates

- [x] Package and metadata validation passes.
- [x] Deterministic source and release-package validation passes.
- [x] Mod Menu integration and public-copy regressions pass.
- [x] Core assertions and mapped JUnit tests pass.
- [x] All required dedicated-server GameTests pass.
- [x] Client GUI and authority GameTests pass.
- [x] The Java 25 Fabric Loom build completes successfully.
- [x] Gradle selects the installed Java 25 compiler when invoked from the host's Java 21 runtime, with automatic download fallback when a matching JDK is absent.
- [x] The exact `512x512` icon source passes clean-checkout GitHub Actions run `33466903620` on commit `a8fe39a9d591d314ee4467ad8dae97edf84ac252`.
- [x] The JAR is inspected for name, version, stable mod ID, approved icon, embedded MIT license, package contents, and absence of GameTest or nested-JAR leakage.

The guarded tag workflow repeats the complete chain against the exact release commit before it can publish any asset.

## Publication gates

- [x] Set `release_ready=true` only in the fully verified release commit.
- [x] Create tag `v1.2.3` on the release commit contained in `main`.
- [x] Rebuild and publish the JAR, source archive, release bundle, build-status record, and checksums only after all automated gates pass.
- [x] Publish the JAR as `smart-resource-multiplier-1.2.3.jar`.
- [x] Tell users to remove `smart-resource-multiplier-1.2.2.jar` before upgrading.

## Manual scope decision

No new gameplay matrix is required for this release because no gameplay, mixin target, configuration field, default, network payload, permission path, persistence identifier, or rule-resolution behavior changed. The existing `1.2.0` through `1.2.2` gameplay evidence remains applicable. Real client and dedicated-server tests still exercise runtime initialization, commands, metadata, and the packaged artifact identity.
