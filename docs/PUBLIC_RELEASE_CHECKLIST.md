# Smart Resource Multiplier public release checklist

## Smart Resource Multiplier 1.2.2

This patch is limited to public branding, metadata, artifact names, documentation, and the regression contracts that verify those surfaces. Gameplay, configuration semantics, schema, networking, permissions, persistence, and anti-duplication behavior are unchanged.

## Branding and compatibility gates

- [x] Fabric and Mod Menu expose the public name **Smart Resource Multiplier**.
- [x] Configuration screens, reset prompts, commands, diagnostics, issue forms, and maintained documentation use the new public name.
- [x] The Fabric mod ID remains `smart_resource_drops`.
- [x] `config/smart_resource_drops.json`, schema 3, datapack and network namespaces, saved provenance, Java packages, `/smartdrops`, and `/smartdropsgui` remain unchanged.
- [x] New build artifacts use `smart-resource-multiplier-<version>.jar`.
- [x] Deterministic release packages use the `SmartResourceMultiplier-*` prefix.
- [x] Upgrade documentation tells players to remove the older JAR before installing the renamed JAR.
- [x] The production icon remained byte-for-byte unchanged for the published `v1.2.2` release; current post-release `main` now uses the separately approved Smart logo documented in [`BRANDING.md`](BRANDING.md).

## Metadata and documentation gates

- [x] `mod_version` is `1.2.2`.
- [x] The fully verified release commit sets `release_ready=true`.
- [x] Fabric `contact.homepage` remains the official CurseForge project.
- [x] Fabric `contact.issues` remains GitHub Issues.
- [x] Fabric `contact.sources` remains the public GitHub repository.
- [x] README, changelog, implementation log, build status, release notes, validators, and package tests document the transition.
- [x] Historical release and verification records preserve the public names and artifact identities that were true for those releases.

## Automated gates

- [x] Package and metadata validation passes.
- [x] Deterministic source and release-package validation passes.
- [x] Mod Menu integration and public-copy regressions pass.
- [x] Core assertions and mapped JUnit tests pass.
- [x] All required dedicated-server GameTests pass.
- [x] Client GUI and authority GameTests pass.
- [x] The Java 25 Fabric Loom build completes successfully.
- [x] The exact rebrand source passes clean-checkout GitHub Actions.
- [x] The JAR is inspected for name, version, stable mod ID, unchanged icon, embedded MIT license, package contents, and absence of GameTest or nested-JAR leakage.

Verification evidence before stable promotion: clean-checkout run `33439853561` on commit `79d6be129d99ce15053f69cf6f242c0350e187b2`; documentation-evidence run `33440180986` on commit `749f8b2ad76d88c5b60ec17d4187558d978c2c38`; playable pre-release JAR SHA-256 `08fe1fb05684b336d71a0f3467a358594148556295624a2a3b42d40325a905fe`; icon SHA-256 `b8a56ed24db3a2e812271d69fd021a5756469ac0d649ebd7cc3f205d7d276694`.

## Publication gates

- [x] Set `release_ready=true` only in the fully verified release commit.
- [x] Create tag `v1.2.2` on the release commit contained in `main`.
- [x] Rebuild and publish the JAR, source archive, release bundle, build-status record, and checksums only after all automated gates pass.
- [x] Publish the JAR as `smart-resource-multiplier-1.2.2.jar`.
- [x] Tell users to remove `resource-multiplier-1.2.1.jar` before upgrading.

## Manual scope decision

No new gameplay matrix is required for this release because no gameplay, mixin target, configuration field, default, network payload, permission path, persistence identifier, or rule-resolution behavior changed. The existing `1.2.0` and `1.2.1` gameplay evidence remains applicable. Real client and dedicated-server tests still exercise the renamed public titles, reset copy, commands, metadata, and packaged artifact identity.
