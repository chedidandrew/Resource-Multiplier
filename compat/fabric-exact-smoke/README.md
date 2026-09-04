# Fabric exact packaged-JAR smoke test

This isolated harness starts a real Fabric dedicated server and loads the supplied
candidate JAR without rebuilding or remapping it. Its default target is Minecraft
1.21.4 with Fabric Loader 0.19.5 and Fabric API 0.119.4+1.21.4.

Run it from the repository root after producing or preserving the candidate:

```powershell
.\compat\fabric-exact-smoke\Run-FabricExactSmoke.ps1 `
  -CandidateJar ".\compat\candidates\smart-resource-multiplier-fabric-1.3.2+mc1.21.4.jar" `
  -AcceptEula
```

`-AcceptEula` confirms that the person running the command has reviewed and
accepted the [Minecraft EULA](https://aka.ms/MinecraftEULA).

For every run, the script:

- creates a unique directory under `runtime/fabric-1.21.4/`;
- copies the candidate into `mods/` and checks SHA-256 before and after launch;
- obtains the server launcher from Fabric Meta and Fabric API from Fabric Maven;
- waits for both the mod initialization message and the server ready message;
- sends `stop`, requires a clean exit, and scans for binary-linkage/mixin failures;
- writes `evidence/smoke-result.json` and `evidence/console.log`.

The `cache/` and `runtime/` directories are generated evidence/downloads and are
ignored by the adjacent `.gitignore`.

## Scope

A pass proves that the exact packaged JAR can be discovered, linked, initialized,
and cleanly stopped on a Fabric 1.21.4 dedicated server. It does not exercise the
client configuration GUI and is not, by itself, proof of gameplay equivalence.
Those are covered separately by the GameTests, client smoke, and multiplayer
authority smoke.
