# Resource Multiplier public release checklist

## Resource Multiplier 1.2.1

This patch changes storefront, issue-tracker, source, and optional funding metadata only. It does not change gameplay or configuration behavior.

## Metadata and documentation gates

- [x] `mod_version` is `1.2.1` and `release_ready=true` only in the intended release commit.
- [x] Fabric `contact.homepage` points to the official CurseForge project.
- [x] Fabric `contact.issues` points to GitHub Issues.
- [x] Fabric `contact.sources` points to the GitHub repository.
- [x] Mod Menu custom links expose Ko-fi, PayPal, and Cash App with localized labels.
- [x] `.github/FUNDING.yml` exposes the same optional support destinations through GitHub.
- [x] README, changelog, implementation log, issue forms, packaging policy, and release records identify `1.2.1`.
- [x] Compatibility identifiers, schema, config path, commands, network IDs, and saved provenance remain unchanged.

## Automated gates

- [x] Package and metadata validation passes.
- [x] Deterministic source and release-package validation passes.
- [x] Mod Menu integration and contact-link regression checks pass.
- [x] Core assertions and mapped JUnit pass.
- [x] All required dedicated-server GameTests pass.
- [x] Client GUI and authority GameTests pass.
- [x] The Java 25 Fabric Loom build completes successfully.
- [ ] The exact final `main` commit passes clean-checkout Build and verify.
- [ ] Annotated tag `v1.2.1` points to the exact tested release commit.
- [ ] The guarded release workflow publishes the JAR and checksum records.

## Manual scope decision

No new gameplay matrix is required because no gameplay, mixin, configuration, networking, persistence, or authority code changed. The complete `1.2.0` manual release evidence remains archived under `docs/archive/`. The `1.2.1` release still requires inspection of the packaged metadata and successful automated client validation.

## Third-party compatibility policy

Third-party compatibility remains case-specific. Reports should include the exact project version, loader, registry IDs, diagnostic output, reproduction steps, and relevant logs.
