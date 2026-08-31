# GitHub publication guide

The canonical public repository is [chedidandrew/Resource-Multiplier](https://github.com/chedidandrew/Resource-Multiplier), with ordinary bug and compatibility reports handled through [GitHub Issues](https://github.com/chedidandrew/Resource-Multiplier/issues). Resource Multiplier `1.2.0` is the stable Fabric 26.2 release line. The exact release commit uses `release_ready=true`; ordinary development commits must keep the latch disabled.

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

The stable JAR must be rebuilt from source. Never rename `resource-multiplier-1.2.0-rc.1.jar` to `resource-multiplier-1.2.0.jar`, because renaming does not update embedded metadata or checksums.

After the final `main` commit passes Build and verify, tag it as `v1.2.0`. The guarded release workflow verifies `release_ready=true`, main ancestry, and tag/version equality, then rebuilds, tests, packages, and publishes the release.

After publication, set `release_ready=false` in the first commit of the next development cycle.
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
