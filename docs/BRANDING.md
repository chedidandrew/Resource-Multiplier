# Smart Resource Multiplier branding

## Current production icon

The current `main` branch uses the approved **SMART RESOURCE MULTIPLIER** icon selected on 2026-08-31. The artwork presents the complete project name beneath a diamond-ore block, diamond pickaxe, scattered diamonds, and two upward multiplier arrows in a dark, Minecraft-inspired frame.

The production asset is a higher-detail `512x512` PNG. This preserves the supplied artwork's fine lighting, pixel texture, and wordmark detail while keeping the packaged asset substantially smaller than the `1254x1254` source.

- Path: `src/main/resources/assets/smart_resource_drops/icon.png`
- Dimensions: `512x512`
- SHA-256: `db216ccd6058404de18f797ebb5be87a313899a27c3f1971fdf086b8637dc190`
- Core visual elements: `SMART RESOURCE MULTIPLIER`, diamond-ore block, diamond pickaxe, scattered diamonds, two upward green arrows, purple-and-gold frame, shader-style highlights

The approved `1254x1254` artwork was downscaled to `512x512` with high-quality Lanczos resampling. Package validation locks both the higher-resolution dimensions and exact checksum.

## Release-history boundary

GitHub Release `v1.2.2` was published before this visual refresh and remains historical release evidence. Its packaged icon is the earlier pre-refresh artwork with SHA-256 `b8a56ed24db3a2e812271d69fd021a5756469ac0d649ebd7cc3f205d7d276694`.

This post-release change updates current `main` branding only. It does not change gameplay, the `smart_resource_drops` mod ID, configuration paths or schema, commands, datapack/network namespace, saved-world provenance, permissions, or anti-duplication behavior. Any future binary published from current `main` should use a version later than `1.2.2` rather than replacing the already published `v1.2.2` artifact.
