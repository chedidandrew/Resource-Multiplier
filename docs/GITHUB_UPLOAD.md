# GitHub publication guide

The canonical repository is [chedidandrew/Resource-Multiplier](https://github.com/chedidandrew/Resource-Multiplier), and player downloads are published on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/resource-multiplier). Minecraft 26.2 remains the newest/default line on `main`.

## This release lane

- Branch: `backport/1.21.5`
- Tag: `v1.3.2+mc1.21.5`
- Fabric asset: `smart-resource-multiplier-1.3.2+mc1.21.5.jar`
- NeoForge asset: `smart-resource-multiplier-neoforge-1.3.2+mc1.21.5.jar`

The stable source commit uses the same `mod_version` in both Gradle property files and sets `release_ready=true`. The guarded release workflow verifies the exact tag, remote branch tip, full dual-loader test chain, and deterministic package before publishing. It uses `make_latest: false` so Minecraft 26.2 remains GitHub's **Latest** release.

Never rename an older JAR: its embedded loader metadata and version would remain wrong. Upload the two JARs as separate CurseForge files with the correct loader and Minecraft 1.21.5 selection. Never install both loader files together.

Ordinary changes run `.github/workflows/build.yml`; tag pushes are handled only by `.github/workflows/release.yml`. Report bugs through [GitHub Issues](https://github.com/chedidandrew/Resource-Multiplier/issues).
