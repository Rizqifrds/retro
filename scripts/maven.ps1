$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$localJdk = Get-ChildItem '.tools/jdk21' -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path (Join-Path $_.FullName 'bin/javac.exe') } | Select-Object -First 1
if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName; $env:PATH = "$($env:JAVA_HOME)/bin;$env:PATH" }
$ProgressPreference = 'SilentlyContinue'
$mavenVersion = '3.9.9'
$mavenRoot = Join-Path (Get-Location) ".tools/apache-maven-$mavenVersion"
$mavenExe = Join-Path $mavenRoot 'bin/mvn.cmd'
if (-not (Test-Path -LiteralPath $mavenExe)) {
    New-Item -ItemType Directory -Force '.tools' | Out-Null
    $archive = Join-Path (Get-Location) ".tools/maven-$mavenVersion.zip"
    Write-Host 'Menyiapkan Maven (unduhan pertama saja)...'
    Invoke-WebRequest "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip" -OutFile $archive
    Expand-Archive -LiteralPath $archive -DestinationPath '.tools' -Force
}
& $mavenExe @args
exit $LASTEXITCODE
