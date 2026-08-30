$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    throw 'JAVA_HOME must point to a Java 25 JDK.'
}
$javac = Join-Path $env:JAVA_HOME 'bin\javac.exe'
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javac) -or -not (Test-Path -LiteralPath $java)) {
    throw "JAVA_HOME does not contain a complete JDK: $env:JAVA_HOME"
}

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
