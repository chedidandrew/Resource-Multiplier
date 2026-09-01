# Smart Resource Multiplier branding

## Current production icon

The current `main` branch uses the approved **SMART x2** icon selected on 2026-08-31. The artwork keeps the established resource-multiplier identity while replacing the previous `RESOURCE MULTIPLIER` wordmark with `SMART` and adding restrained shader-style lighting, depth, highlights, and glow.

The production asset remains a compact `128x128` PNG because that is the project contract enforced by package validation and is sufficient for Minecraft, Mod Menu, GitHub README rendering, and storefront thumbnails.

- Path: `src/main/resources/assets/smart_resource_drops/icon.png`
- Dimensions: `128x128`
- SHA-256: `a62a59890d670c4df1ebca6375387bd47736e510cc1a755abd3ecb8bd4d24642`
- Core visual elements: `SMART`, white `x2`, two upward green arrows, diamond, coal, iron ingot, log, grass-and-dirt frame, shader-style highlights

The approved high-resolution artwork was downscaled to the required production size with high-quality resampling and light edge sharpening so the shader detail survives thumbnail rendering without materially increasing the packaged mod size.

## Release-history boundary

GitHub Release `v1.2.2` was published before this visual refresh and remains historical release evidence. Its packaged icon is the earlier pre-refresh artwork with SHA-256 `b8a56ed24db3a2e812271d69fd021a5756469ac0d649ebd7cc3f205d7d276694`.

This post-release change updates current `main` branding only. It does not change gameplay, the `smart_resource_drops` mod ID, configuration paths or schema, commands, datapack/network namespace, saved-world provenance, permissions, or anti-duplication behavior. Any future binary published from current `main` should use a version later than `1.2.2` rather than replacing the already published `v1.2.2` artifact.
