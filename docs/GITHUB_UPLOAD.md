# GitHub publication guide

The canonical public source repository is [chedidandrew/Resource-Multiplier](https://github.com/chedidandrew/Resource-Multiplier). User downloads are published on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/resource-multiplier), and ordinary bug or compatibility reports are handled through [GitHub Issues](https://github.com/chedidandrew/Resource-Multiplier/issues). Minecraft 26.2 remains the newest/default line on `main`; the maintained `backport/1.21.1` branch provides separate Fabric and NeoForge `1.3.0+mc1.21.1` files.

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

`.github/workflows/build.yml` runs on branch pushes, pull requests, and manual dispatch. This backport uses Java 21 to validate both loader builds, run the shared and loader-specific JUnit/GameTest suites, exercise real client GUI and multiplayer authority paths under Xvfb, and audit both playable JARs. Build artifacts remain loader-labelled. Tag pushes are handled only by the guarded release workflow.

## Release

Always rebuild both versioned JARs from source. Renaming an older JAR does not update embedded loader metadata, manifests, package records, or checksums.

A stable source commit must set the same `mod_version` in both Gradle property files and set `release_ready=true`. The 1.21.1 source remains on `backport/1.21.1`; it is never merged into `main`. Tag `v1.3.0+mc1.21.1` invokes the guarded release workflow, which verifies the latch, version equality, backport-branch ancestry, full dual-loader test chain, and deterministic package before publishing. The workflow explicitly uses `make_latest: false`, so the Minecraft 26.2 `v1.3.0` release remains GitHub's **Latest** release.

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
