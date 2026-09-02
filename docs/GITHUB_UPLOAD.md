# GitHub publication guide

The canonical public source repository is [chedidandrew/Resource-Multiplier](https://github.com/chedidandrew/Resource-Multiplier). User downloads are published on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/resource-multiplier), and ordinary bug or compatibility reports are handled through [GitHub Issues](https://github.com/chedidandrew/Resource-Multiplier/issues). Smart Resource Multiplier `1.3.0` provides separate Fabric and NeoForge files for Minecraft 26.2.

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

`.github/workflows/build.yml` runs on branch pushes, pull requests, and manual dispatch. With Java 25 it validates both loader builds, runs the shared and loader-specific JUnit/GameTest suites, exercises real client GUI and multiplayer authority paths under Xvfb, audits both playable JARs, and verifies the one-way Fabric-to-NeoForge provenance migration. Build artifacts remain loader-labelled. Tag pushes are handled only by the guarded release workflow.

## Release

Always rebuild both versioned JARs from source. Renaming an older JAR does not update embedded loader metadata, manifests, package records, or checksums.

A stable source commit must set the same `mod_version` in both Gradle property files and set `release_ready=true`. Pushing that commit to `main` updates the public source without creating a GitHub Release. A matching `v<version>` tag is a separate, optional action: only that tag invokes the guarded GitHub-release workflow, which verifies the latch, version equality, main ancestry, full dual-loader test chain, and deterministic package before publishing.

## Public links

- Website and primary download page: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/resource-multiplier)
- Source repository: [GitHub](https://github.com/chedidandrew/Resource-Multiplier)
- Bug and compatibility reports: [GitHub Issues](https://github.com/chedidandrew/Resource-Multiplier/issues)
- Optional support: [Ko-fi](https://ko-fi.com/andrewchedid), [PayPal](https://www.paypal.com/paypalme/chedidandrew), and [Cash App](https://cash.app/%24AndrewChedid)

The same destinations are locked by package validation and exposed through loader metadata and optional Mod Menu integration. `.github/FUNDING.yml` supplies GitHub's sponsor button.

## Repository presentation and settings checklist

- Use this concise repository description: **Configurable multipliers for block drops, mob loot, and supported shearing, with persistent anti-duplication protection.**
- Set the repository homepage to the official CurseForge project page.
- Keep Issues enabled and Discussions disabled for the current support model.
- Disable the Wiki while `docs/` remains canonical.
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
neoforge
java
loot
server-side
anti-dupe
smart-resource-multiplier
```
