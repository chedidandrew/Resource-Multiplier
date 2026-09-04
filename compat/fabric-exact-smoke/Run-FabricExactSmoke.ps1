[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $CandidateJar,

    [string] $MinecraftVersion = "1.21.4",
    [string] $LoaderVersion = "0.19.5",
    [string] $FabricApiVersion = "0.119.4+1.21.4",
    [string] $JavaExecutable = "java",
    [ValidateRange(60, 900)]
    [int] $StartupTimeoutSeconds = 300,
    [switch] $AcceptEula
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (-not $AcceptEula) {
    throw "The Minecraft server requires EULA acceptance. Review https://aka.ms/MinecraftEULA and rerun with -AcceptEula."
}

$candidate = (Resolve-Path -LiteralPath $CandidateJar).Path
if ([IO.Path]::GetExtension($candidate) -ne ".jar") {
    throw "CandidateJar must point to a .jar file: $candidate"
}

$scriptRoot = $PSScriptRoot
$cacheRoot = Join-Path $scriptRoot "cache"
$runsRoot = Join-Path $scriptRoot "runtime"
$runId = "{0}-{1}" -f (Get-Date -Format "yyyyMMdd-HHmmss"), ([guid]::NewGuid().ToString("N").Substring(0, 8))
$runRoot = Join-Path (Join-Path $runsRoot ("fabric-" + $MinecraftVersion)) $runId
$modsRoot = Join-Path $runRoot "mods"
$evidenceRoot = Join-Path $runRoot "evidence"

[IO.Directory]::CreateDirectory($cacheRoot) | Out-Null
[IO.Directory]::CreateDirectory($modsRoot) | Out-Null
[IO.Directory]::CreateDirectory($evidenceRoot) | Out-Null

function Get-Sha256([string] $Path) {
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}

function Invoke-Download([string] $Uri, [string] $Destination) {
    if (Test-Path -LiteralPath $Destination) {
        return
    }

    $partial = "$Destination.partial"
    try {
        Invoke-WebRequest -UseBasicParsing -Uri $Uri -OutFile $partial
        Move-Item -LiteralPath $partial -Destination $Destination
    }
    finally {
        if (Test-Path -LiteralPath $partial) {
            Remove-Item -LiteralPath $partial -Force
        }
    }
}

function Read-FabricMetadata([string] $JarPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $entry = $archive.GetEntry("fabric.mod.json")
        if ($null -eq $entry) {
            throw "Candidate does not contain fabric.mod.json: $JarPath"
        }

        $reader = [IO.StreamReader]::new($entry.Open())
        try {
            return ($reader.ReadToEnd() | ConvertFrom-Json)
        }
        finally {
            $reader.Dispose()
        }
    }
    finally {
        $archive.Dispose()
    }
}

function Add-ProcessLine(
    [string] $Line,
    [string] $StreamName,
    [Collections.Generic.List[string]] $Lines,
    [Collections.Generic.List[string]] $Failures,
    [ref] $ServerReady,
    [ref] $ModInitialized
) {
    if ($null -eq $Line) {
        return
    }

    $rendered = "[$StreamName] $Line"
    $Lines.Add($rendered)
    [Console]::WriteLine($rendered)

    if ($Line -match "Smart Resource Multiplier initialized") {
        $ModInitialized.Value = $true
    }
    if ($Line -match "Done \([0-9.,]+s\)! For help, type") {
        $ServerReady.Value = $true
    }

    $fatalPatterns = @(
        "Could not execute entrypoint",
        "Mixin apply for mod smart_resource_drops failed",
        "Mixin transformation of .* failed",
        "InvalidMixinException",
        "NoClassDefFoundError",
        "NoSuchMethodError",
        "AbstractMethodError",
        "UnsupportedClassVersionError"
    )
    foreach ($pattern in $fatalPatterns) {
        if ($Line -match $pattern) {
            $Failures.Add($rendered)
            break
        }
    }
}

$metadata = Read-FabricMetadata $candidate
if ($metadata.id -ne "smart_resource_drops") {
    throw "Unexpected Fabric mod id '$($metadata.id)' in $candidate"
}

$candidateHashBefore = Get-Sha256 $candidate
$candidateCopy = Join-Path $modsRoot ([IO.Path]::GetFileName($candidate))
Copy-Item -LiteralPath $candidate -Destination $candidateCopy
$candidateCopyHashBefore = Get-Sha256 $candidateCopy
if ($candidateCopyHashBefore -ne $candidateHashBefore) {
    throw "Candidate copy SHA-256 mismatch before launch."
}

$installerVersionsUri = "https://meta.fabricmc.net/v2/versions/installer"
$installerVersions = Invoke-RestMethod -UseBasicParsing -Uri $installerVersionsUri
$installer = $installerVersions | Where-Object { $_.stable } | Select-Object -First 1
if ($null -eq $installer) {
    throw "Fabric Meta did not return a stable installer version."
}
$installerVersion = [string] $installer.version

$launcherName = "fabric-server-mc.$MinecraftVersion-loader.$LoaderVersion-installer.$installerVersion.jar"
$launcherJar = Join-Path $cacheRoot $launcherName
$launcherUri = "https://meta.fabricmc.net/v2/versions/loader/$MinecraftVersion/$LoaderVersion/$installerVersion/server/jar"
Invoke-Download $launcherUri $launcherJar

$encodedApiVersion = [Uri]::EscapeDataString($FabricApiVersion)
$apiName = "fabric-api-$FabricApiVersion.jar"
$apiJar = Join-Path $cacheRoot $apiName
$apiUri = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$encodedApiVersion/fabric-api-$encodedApiVersion.jar"
Invoke-Download $apiUri $apiJar
Copy-Item -LiteralPath $apiJar -Destination (Join-Path $modsRoot $apiName)

$eulaPath = Join-Path $runRoot "eula.txt"
[IO.File]::WriteAllText($eulaPath, "eula=true`n", [Text.UTF8Encoding]::new($false))
$serverPropertiesPath = Join-Path $runRoot "server.properties"
$serverProperties = @(
    "level-name=world",
    "max-tick-time=-1",
    "motd=Smart Resource Multiplier exact-JAR smoke test",
    "server-port=0",
    "simulation-distance=2",
    "view-distance=2"
) -join "`n"
[IO.File]::WriteAllText($serverPropertiesPath, "$serverProperties`n", [Text.UTF8Encoding]::new($false))

$consoleLog = Join-Path $evidenceRoot "console.log"
$resultPath = Join-Path $evidenceRoot "smoke-result.json"
$startedAt = [DateTimeOffset]::UtcNow
$allLines = [Collections.Generic.List[string]]::new()
$failures = [Collections.Generic.List[string]]::new()
$serverReady = $false
$modInitialized = $false
$exitCode = $null
$stopSent = $false
$timedOut = $false

$processInfo = [Diagnostics.ProcessStartInfo]::new()
$processInfo.FileName = $JavaExecutable
$processInfo.WorkingDirectory = $runRoot
$processInfo.UseShellExecute = $false
$processInfo.RedirectStandardInput = $true
$processInfo.RedirectStandardOutput = $true
$processInfo.RedirectStandardError = $true
$processInfo.CreateNoWindow = $true
$processInfo.ArgumentList.Add("-Xms512M")
$processInfo.ArgumentList.Add("-Xmx1G")
$processInfo.ArgumentList.Add("-Dfabric.skipMcProvider=false")
$processInfo.ArgumentList.Add("-jar")
$processInfo.ArgumentList.Add($launcherJar)
$processInfo.ArgumentList.Add("nogui")

$process = [Diagnostics.Process]::new()
$process.StartInfo = $processInfo

try {
    if (-not $process.Start()) {
        throw "Java process did not start."
    }

    $stdoutTask = $process.StandardOutput.ReadLineAsync()
    $stderrTask = $process.StandardError.ReadLineAsync()
    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($StartupTimeoutSeconds)

    while (-not $process.HasExited -and -not $serverReady -and [DateTimeOffset]::UtcNow -lt $deadline) {
        if ($stdoutTask.IsCompleted) {
            $line = $stdoutTask.GetAwaiter().GetResult()
            Add-ProcessLine $line "stdout" $allLines $failures ([ref] $serverReady) ([ref] $modInitialized)
            $stdoutTask = $process.StandardOutput.ReadLineAsync()
        }
        if ($stderrTask.IsCompleted) {
            $line = $stderrTask.GetAwaiter().GetResult()
            Add-ProcessLine $line "stderr" $allLines $failures ([ref] $serverReady) ([ref] $modInitialized)
            $stderrTask = $process.StandardError.ReadLineAsync()
        }
        Start-Sleep -Milliseconds 25
    }

    if (-not $serverReady -and -not $process.HasExited) {
        $timedOut = $true
    }

    if (-not $process.HasExited) {
        $process.StandardInput.WriteLine("stop")
        $process.StandardInput.Flush()
        $stopSent = $true
        if (-not $process.WaitForExit(60000)) {
            $process.Kill($true)
            $process.WaitForExit()
            $failures.Add("Server did not stop within 60 seconds after the stop command.")
        }
    }

    while ($true) {
        $line = $stdoutTask.GetAwaiter().GetResult()
        if ($null -eq $line) { break }
        Add-ProcessLine $line "stdout" $allLines $failures ([ref] $serverReady) ([ref] $modInitialized)
        $stdoutTask = $process.StandardOutput.ReadLineAsync()
    }
    while ($true) {
        $line = $stderrTask.GetAwaiter().GetResult()
        if ($null -eq $line) { break }
        Add-ProcessLine $line "stderr" $allLines $failures ([ref] $serverReady) ([ref] $modInitialized)
        $stderrTask = $process.StandardError.ReadLineAsync()
    }

    $exitCode = $process.ExitCode
}
finally {
    if (-not $process.HasExited) {
        $process.Kill($true)
        $process.WaitForExit()
    }
    $process.Dispose()
}

[IO.File]::WriteAllLines($consoleLog, $allLines, [Text.UTF8Encoding]::new($false))

$candidateHashAfter = Get-Sha256 $candidate
$candidateCopyHashAfter = Get-Sha256 $candidateCopy
$hashPreserved = (
    $candidateHashBefore -eq $candidateHashAfter -and
    $candidateHashBefore -eq $candidateCopyHashBefore -and
    $candidateHashBefore -eq $candidateCopyHashAfter
)

$passed = (
    $serverReady -and
    $modInitialized -and
    -not $timedOut -and
    $exitCode -eq 0 -and
    $failures.Count -eq 0 -and
    $hashPreserved
)

$result = [ordered]@{
    passed = $passed
    scope = "Exact packaged-JAR dedicated-server startup smoke test; not a client GUI or gameplay-equivalence test."
    startedAtUtc = $startedAt.ToString("o")
    finishedAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    minecraftVersion = $MinecraftVersion
    fabricLoaderVersion = $LoaderVersion
    fabricInstallerVersion = $installerVersion
    fabricApiVersion = $FabricApiVersion
    modId = [string] $metadata.id
    modVersion = [string] $metadata.version
    declaredMinecraftRange = [string] $metadata.depends.minecraft
    candidateSource = $candidate
    candidateRuntimeCopy = $candidateCopy
    candidateSha256Before = $candidateHashBefore
    candidateSha256After = $candidateHashAfter
    runtimeCopySha256Before = $candidateCopyHashBefore
    runtimeCopySha256After = $candidateCopyHashAfter
    candidateHashPreserved = $hashPreserved
    launcherSha256 = Get-Sha256 $launcherJar
    fabricApiSha256 = Get-Sha256 $apiJar
    serverReachedReady = $serverReady
    modInitializationObserved = $modInitialized
    stopCommandSent = $stopSent
    timedOut = $timedOut
    exitCode = $exitCode
    fatalLogMatches = @($failures)
    consoleLog = $consoleLog
    minecraftLog = Join-Path $runRoot "logs\latest.log"
}

[IO.File]::WriteAllText(
    $resultPath,
    ($result | ConvertTo-Json -Depth 6),
    [Text.UTF8Encoding]::new($false)
)

Write-Host ""
Write-Host "Evidence: $resultPath"
Write-Host "Candidate SHA-256: $candidateHashBefore"
if (-not $passed) {
    throw "Fabric exact-JAR smoke test failed. Review $resultPath and $consoleLog"
}

Write-Host "PASS: exact candidate JAR loaded and the Fabric $MinecraftVersion server reached ready state."
