$ErrorActionPreference = 'Stop'

function Test-Java25Jdk([string] $candidate) {
    if ([string]::IsNullOrWhiteSpace($candidate)) {
        return $false
    }
    $candidateJavac = Join-Path $candidate 'bin\javac.exe'
    $candidateJava = Join-Path $candidate 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $candidateJavac) -or -not (Test-Path -LiteralPath $candidateJava)) {
        return $false
    }
    $version = (& $candidateJavac -version 2>&1 | Select-Object -First 1).ToString()
    return $version -match '^javac 25(?:\.|$)'
}

$java25Candidates = @($env:JAVA_25_HOME, $env:JAVA_HOME)
foreach ($base in @('C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium')) {
    if (Test-Path -LiteralPath $base) {
        $java25Candidates += Get-ChildItem -LiteralPath $base -Directory -Filter 'jdk-25*' |
            Sort-Object Name -Descending |
            Select-Object -ExpandProperty FullName
    }
}
$resolvedJavaHome = $java25Candidates |
    Where-Object { Test-Java25Jdk $_ } |
    Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($resolvedJavaHome)) {
    throw 'Java 25 is required. Install a Java 25 JDK or set JAVA_25_HOME/JAVA_HOME to it.'
}
$javac = Join-Path $resolvedJavaHome 'bin\javac.exe'
$java = Join-Path $resolvedJavaHome 'bin\java.exe'

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$output = [System.IO.Path]::GetFullPath((Join-Path $repoRoot '.build\core-tests'))
$requiredPrefix = $repoRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
if (-not $output.StartsWith($requiredPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to clean an output path outside the repository: $output"
}
if (Test-Path -LiteralPath $output) {
    Remove-Item -LiteralPath $output -Recurse -Force
}
New-Item -ItemType Directory -Path $output | Out-Null

$sources = @(
    'src\main\java\com\chedidandrew\smartresourcedrops\config\ConfigLoadDiagnostics.java',
    'src\main\java\com\chedidandrew\smartresourcedrops\config\SmartDropsConfig.java',
    'src\main\java\com\chedidandrew\smartresourcedrops\core\entity\EntityCategory.java',
    'src\main\java\com\chedidandrew\smartresourcedrops\core\Category.java',
    'src\main\java\com\chedidandrew\smartresourcedrops\core\DropSource.java',
    'src\main\java\com\chedidandrew\smartresourcedrops\core\PackedBlockPosition.java',
    'src\main\java\com\chedidandrew\smartresourcedrops\core\RuleResolutionTrace.java',
    'src\main\java\com\chedidandrew\smartresourcedrops\core\RuleEngine.java',
    'tools\core-tests\com\chedidandrew\smartresourcedrops\core\RuleEngineTest.java'
) | ForEach-Object { Join-Path $repoRoot $_ }

& $javac --release 25 -Xlint:deprecation -d $output @sources
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}
& $java -cp $output com.chedidandrew.smartresourcedrops.core.RuleEngineTest
if ($LASTEXITCODE -ne 0) {
    throw "Core tests failed with exit code $LASTEXITCODE"
}
