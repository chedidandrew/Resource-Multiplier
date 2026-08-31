# Resource Multiplier public release checklist

## Resource Multiplier 1.2.1

This patch changes storefront, issue-tracker, source, and optional funding metadata only. It does not change gameplay or configuration behavior.

## Metadata and documentation gates

- [x] `mod_version` is `1.2.1` and the tagged release source authorizes publication with `release_ready=true`.
- [x] Fabric `contact.homepage` points to the official CurseForge project.
- [x] Fabric `contact.issues` points to GitHub Issues.
- [x] Fabric `contact.sources` points to the GitHub repository.
- [x] Mod Menu custom links expose Ko-fi, PayPal, and Cash App with localized labels.
- [x] `.github/FUNDING.yml` exposes the same optional support destinations through GitHub.
- [x] README, changelog, implementation log, issue forms, packaging policy, and release records identify `1.2.1`.
- [x] Compatibility identifiers, schema, config path, commands, network IDs, and saved provenance remain unchanged.

## Automated and publication gates

- [x] Package and metadata validation passes.
- [x] Deterministic source and release-package validation passes.
- [x] Mod Menu integration and contact-link regression checks pass.
- [x] Core assertions and mapped JUnit pass.
- [x] All 66 required dedicated-server GameTests pass.
- [x] Client GUI and authority GameTests pass.
- [x] The Java 25 Fabric Loom build completes successfully.
- [x] The exact tested release commit `34cc5a64c6d97c8db332ce45260c7615a8f9ab97` passes clean-checkout Build and verify in run `33419561475`.
- [x] Annotated tag `v1.2.1` points to the exact tested release commit.
- [x] The guarded release workflow passes in run `33419972111`.
- [x] GitHub Release `v1.2.1` publishes the playable JAR, source archive, release bundle, build-status record, and checksum files.
- [x] Official playable JAR is `601971` bytes with 311 ZIP entries and SHA-256 `6E90578892E1F9AA2BF22B8FE4BE1B7831E8BE87F767D14BDB2C376F0443CA32`.

## Manual scope decision

No new gameplay matrix was required because no gameplay, mixin, configuration, networking, persistence, or authority code changed. The complete `1.2.0` manual release evidence remains archived under `docs/archive/`. The packaged `1.2.1` metadata was inspected through the release validators, and the dedicated-server and real-client validation suites passed before publication.

## Third-party compatibility policy

Third-party compatibility remains case-specific. Reports should include the exact project version, loader, registry IDs, diagnostic output, reproduction steps, and relevant logs.
