# GitHub publication guide

The canonical public source repository is [chedidandrew/Resource-Multiplier](https://github.com/chedidandrew/Resource-Multiplier). User downloads are published on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/resource-multiplier), and ordinary bug or compatibility reports are handled through [GitHub Issues](https://github.com/chedidandrew/Resource-Multiplier/issues). Smart Resource Multiplier `1.2.3` is the current published Fabric 26.2 release.

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

`.github/workflows/build.yml` runs on branch pushes, pull requests, and manual dispatch. With Java 25 it runs the source and package validators, the complete core and mapped-Minecraft JUnit suites, all required dedicated-server GameTests, the Fabric Loom build, and the real client GUI and authority GameTests under Xvfb. The playable JAR, sources JAR, and a clean `git archive` source snapshot are uploaded together as a `SmartResourceMultiplier-<commit>` Actions artifact. Tag pushes are handled only by the guarded release workflow.

## Release

Always rebuild a versioned JAR from source. Renaming an older JAR does not update its embedded Fabric metadata, manifest, package records, or checksums.

A stable release commit must set `mod_version` to the intended version and `release_ready=true`. After the exact `main` commit passes Build and verify, tag that commit with the matching `v<version>` tag. The guarded release workflow verifies the latch, tag-to-version equality, and main ancestry before rebuilding, testing, packaging, and publishing.

## Public links

- Website and primary download page: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/resource-multiplier)
- Source repository: [GitHub](https://github.com/chedidandrew/Resource-Multiplier)
- Bug and compatibility reports: [GitHub Issues](https://github.com/chedidandrew/Resource-Multiplier/issues)
- Optional support: [Ko-fi](https://ko-fi.com/andrewchedid), [PayPal](https://www.paypal.com/paypalme/chedidandrew), and [Cash App](https://cash.app/%24AndrewChedid)

The same destinations are locked by package validation and exposed through Fabric or Mod Menu metadata. `.github/FUNDING.yml` supplies GitHub's sponsor button.

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
java
loot
server-side
anti-dupe
smart-resource-multiplier
```
