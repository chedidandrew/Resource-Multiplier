# GitHub upload guide

The supplied project directory has no `.git` metadata. Its Markdown logs preserve logical release history, but there are no commits, branches, tags, or clean-tree status to recover from this folder. The current tree contains an unreleased shearing candidate and deliberately retains `1.1.0` metadata until its open manual gates pass. Initialize a new repository only if an earlier Git repository cannot be restored, and do not label or tag this candidate as a release.

## First push

```bash
git init
git add .
git commit -m "Import unreleased Resource Multiplier shearing candidate"
git branch -M main
git remote add origin YOUR_REPOSITORY_URL
git push -u origin main
```

## CI

`.github/workflows/build.yml` runs on pushes, pull requests, tags, and manual dispatch. With Java 25 it runs the source/package validators, the complete core and mapped-Minecraft JUnit suites, all required dedicated-server GameTests (66 in the current shearing candidate), the Fabric Loom build, and the real client GUI/authority GameTest under Xvfb. The non-sources JAR is uploaded as an Actions artifact.

## Release

Do not tag the current `1.1.0`-metadata shearing candidate. Complete every open automated, runtime, hands-on, reload, multiplayer, and third-party gate in `docs/PUBLIC_RELEASE_CHECKLIST.md`. Only then bump all required metadata to `1.2.0`, rebuild and re-inspect the final artifacts, confirm the clean-checkout workflow passes, and tag that exact tested commit:

```bash
git tag -a v1.2.0 -m "Resource Multiplier 1.2.0"
git push origin v1.2.0
```

`.github/workflows/release.yml` first verifies that the tag matches `mod_version`, then repeats source-package validation, JUnit, dedicated-server GameTests, the Loom build, and the Xvfb client GameTest. It creates a clean deterministic source ZIP, SHA-256 checksums, and attaches the artifacts to a GitHub release.

## Recommended repository settings

- Protect the `main` branch.
- Require the `Build and verify` check before merge.
- Use pull requests for gameplay changes.
- Keep `CHANGELOG.md` and `docs/IMPLEMENTATION_LOG.md` current.
- Do not commit generated `.gradle`, `build`, `run`, or IDE directories.

## Suggested topics

```text
minecraft
fabric
minecraft-mod
quality-of-life
resource-gathering
server-utility
anti-dupe
```
