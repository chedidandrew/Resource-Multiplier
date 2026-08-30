# GitHub publication guide

The canonical public repository is [chedidandrew/Resource-Multiplier](https://github.com/chedidandrew/Resource-Multiplier), with ordinary bug and compatibility reports handled through [GitHub Issues](https://github.com/chedidandrew/Resource-Multiplier/issues). The current tree contains an unreleased shearing candidate and deliberately retains `1.1.0` metadata until its open manual gates pass. Do not label or tag this candidate as a `1.2.0` release.

## Clone and contribute

```bash
git clone https://github.com/chedidandrew/Resource-Multiplier.git
cd Resource-Multiplier
git switch -c your-change
git add .
git commit -m "Describe the change"
git push -u origin your-change
```

## CI

`.github/workflows/build.yml` runs on pushes, pull requests, tags, and manual dispatch. With Java 25 it runs the source/package validators, the complete core and mapped-Minecraft JUnit suites, all required dedicated-server GameTests (66 in the current shearing candidate), the Fabric Loom build, and the real client GUI/authority GameTest under Xvfb. The non-sources JAR is uploaded as an Actions artifact.

## Release

Do not tag the current `1.1.0`-metadata shearing candidate. `gradle.properties` deliberately contains `release_ready=false`, so the release workflow will refuse to publish even if a matching tag is pushed accidentally.

Complete every open automated, runtime, hands-on, reload, multiplayer, and third-party gate in `docs/PUBLIC_RELEASE_CHECKLIST.md`. Only then bump all required metadata to `1.2.0`, rebuild and re-inspect the final artifacts, and confirm the clean-checkout workflow passes. Set `release_ready=true` only in that exact fully tested release commit, merge it to `main`, and wait for the required `Build and verify` check before tagging that commit:

```bash
git tag -a v1.2.0 -m "Resource Multiplier 1.2.0"
git push origin v1.2.0
```

`.github/workflows/release.yml` checks out full Git history, refuses publication unless `release_ready=true`, verifies that the tagged commit is contained in `origin/main`, and verifies that the tag matches `mod_version`. It then repeats source-package validation, JUnit, dedicated-server GameTests, the Loom build, and the Xvfb client GameTest. It creates a clean deterministic source ZIP, SHA-256 checksums, and attaches the artifacts to a GitHub release.

The packager accepts exactly the Git-tracked source manifest and refuses stray untracked files or a non-empty output directory. For a manual dry run, choose a newly created empty directory; do not reuse the historical `dist/` folder or upload a broad directory glob by hand.

After publication, set `release_ready=false` again in the first commit of the next development cycle. Never leave the latch enabled on an in-progress candidate.

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
