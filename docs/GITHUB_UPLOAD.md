# GitHub publication guide — Minecraft 1.20.1

The canonical repository is [chedidandrew/Resource-Multiplier](https://github.com/chedidandrew/Resource-Multiplier). Minecraft 26.2 remains the newest/default line on `main`. Minecraft 1.20.1 is maintained on `backport/1.20.1` and is published as a stable, non-latest dual-loader release.

## Ordinary development

Work on `backport/1.20.1`, keep `release_ready=false`, and push or open a pull request normally. The build workflow launches Gradle with Java 21 because Fabric Loom requires it, while compiling and running the Minecraft 1.20.1 targets with Java 17. It checks both loaders and does not publish a release.

Expected artifacts:

- `build/libs/smart-resource-multiplier-1.3.0+mc1.20.1.jar` — Fabric
- `neoforge/build/libs/smart-resource-multiplier-neoforge-1.3.0+mc1.20.1.jar` — NeoForge 47

Do not commit generated build directories or rename both files to the same filename.

## Final candidate

Before authorizing publication:

1. Complete every automated and manual item in [TESTING.md](TESTING.md) and [PUBLIC_RELEASE_CHECKLIST.md](PUBLIC_RELEASE_CHECKLIST.md).
2. Rebuild from a clean checkout of the exact candidate commit with Java 21 launching Gradle and its Java 17 toolchain compiling/running the Minecraft targets.
3. Validate both final, playable JARs; do not use a NeoForge `devlibs` artifact.
4. Record final SHA-256 hashes and sizes.
5. Confirm `mod_version=1.3.0+mc1.20.1` in both Gradle property files.
6. Change `release_ready` to `true` only in the exact fully verified commit.
7. Push that commit to `backport/1.20.1`, then create and push tag `v1.3.0+mc1.20.1` at that commit.

The guarded release workflow rejects branch/manual publication, a mismatched tag or loader version, a tag outside `origin/backport/1.20.1`, a false publication latch, or any failing release gate. It uploads exactly the two loader-labelled JARs and uses `make_latest: false`, preserving the Minecraft 26.2 release as GitHub's **Latest** entry.

## CurseForge

Upload the two JARs as separate files under the same project:

- mark the Fabric JAR as Minecraft 1.20.1 / Fabric and require Fabric API;
- mark the NeoForge JAR as Minecraft 1.20.1 / NeoForge;
- use the casual-player changelog in `docs/releases/1.3.0+mc1.20.1.md`;
- never mark either file compatible with both loaders.

Players should remove older copies and install only one Smart Resource Multiplier JAR. This backport does not claim tested Fabric-to-NeoForge world conversion.
