# GitHub publication guide

The canonical public repository is [chedidandrew/Resource-Multiplier](https://github.com/chedidandrew/Resource-Multiplier), with ordinary bug and compatibility reports handled through [GitHub Issues](https://github.com/chedidandrew/Resource-Multiplier/issues). The current tree is Resource Multiplier `1.2.0-rc.1`. It is a development release candidate with open manual gates, no published GitHub Release, and `release_ready=false`; do not label or tag it as final `1.2.0`.

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

`.github/workflows/build.yml` runs on branch pushes, pull requests, and manual dispatch. With Java 25 it runs the source/package validators, the complete core and mapped-Minecraft JUnit suites, all required dedicated-server GameTests (66 in the current shearing candidate), the Fabric Loom build, and the real client GUI/authority GameTest under Xvfb. The non-sources JAR is uploaded as an Actions artifact. Tag pushes are handled only by the authoritative release workflow so the full suite is not duplicated.

## Release

Do not tag the current `1.2.0-rc.1` candidate. `gradle.properties` deliberately contains `release_ready=false`, so the release workflow will refuse to build, package, or publish even if a matching tag is pushed accidentally.

Complete every open automated, runtime, hands-on, reload, multiplayer, and third-party gate in `docs/PUBLIC_RELEASE_CHECKLIST.md`. Only then promote `1.2.0-rc.1` to `1.2.0`, rebuild and re-inspect the final artifacts, and confirm the clean-checkout workflow passes. Set `release_ready=true` only in that exact fully tested release commit, merge it to `main`, and wait for the required `Build and verify` check before tagging that commit:

```bash
git tag -a v1.2.0 -m "Resource Multiplier 1.2.0"
git push origin v1.2.0
```

`.github/workflows/release.yml` checks out full Git history, refuses publication unless `release_ready=true`, verifies that the tagged commit is contained in `origin/main`, and verifies that the tag matches `mod_version`. It then repeats source-package validation, JUnit, dedicated-server GameTests, the Loom build, and the Xvfb client GameTest. It creates a clean deterministic source ZIP, SHA-256 checksums, and attaches the artifacts to a GitHub release.

The packager accepts exactly the Git-tracked source manifest and refuses stray untracked files or a non-empty output directory. For a manual dry run, choose a newly created empty directory; do not reuse the historical `dist/` folder or upload a broad directory glob by hand.

After publication, set `release_ready=false` again in the first commit of the next development cycle. Never leave the latch enabled on an in-progress candidate.

## Repository presentation and settings checklist

These GitHub-side settings are manual repository-administration actions; source validation cannot enforce them:

- Use this concise repository description: **Configurable multipliers for block drops, mob loot, and supported shearing, with persistent anti-duplication protection.**
- Leave the repository homepage field empty until a real project-owned site exists.
- Keep Issues enabled and Discussions disabled for the current support model.
- Disable the Wiki while `docs/` remains the canonical documentation, avoiding two conflicting documentation sets.
- Review Projects and disable it if no board is actively maintained.
- Enable automatic deletion of head branches after pull requests merge.
- Protect `main`, require pull requests for gameplay changes, and require the `Build and verify` check before merge.
- Keep `CHANGELOG.md` and `docs/IMPLEMENTATION_LOG.md` current, and do not commit generated `.gradle`, `build`, `run`, or IDE directories.

## Recommended topics

```text
minecraft
minecraft-mod
fabric
fabricmc
java
loot
server-side
anti-dupe
resource-multiplier
```
