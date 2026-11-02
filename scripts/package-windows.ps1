param([switch]$SkipTests)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$localJdk = Get-ChildItem '.tools/jdk21' -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path (Join-Path $_.FullName 'bin/jpackage.exe') } | Select-Object -First 1
if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName; $env:PATH = "$($env:JAVA_HOME)/bin;$env:PATH" }
$jpackage = Get-Command jpackage -ErrorAction SilentlyContinue
if (-not $jpackage) { throw 'JDK 21 dengan jpackage diperlukan.' }
$buildArgs = @('package')
if ($SkipTests) { $buildArgs += '-DskipTests' }
& '.\mvnw.cmd' @buildArgs
if ($LASTEXITCODE -ne 0) { throw 'Build gagal.' }
$inputDir = Join-Path (Get-Location) 'target/package-input'
New-Item -ItemType Directory -Force $inputDir | Out-Null
Copy-Item -LiteralPath 'target/retroquest-1.0.0.jar' -Destination $inputDir -Force
Copy-Item -LiteralPath 'target/lib' -Destination $inputDir -Recurse -Force
$output = Join-Path (Get-Location) 'dist'
New-Item -ItemType Directory -Force $output | Out-Null
if (Test-Path (Join-Path $output 'RetroQuest')) { throw 'dist/RetroQuest sudah ada. Pindahkan folder lama terlebih dahulu sebelum membuat paket baru.' }
& $jpackage.Source --type app-image --name RetroQuest --app-version 1.0.0 --vendor 'RetroQuest PENS' --input $inputDir --main-jar retroquest-1.0.0.jar --main-class id.ac.pens.retroquest.Launcher --dest $output --java-options '-Dprism.order=sw' --java-options '--enable-native-access=ALL-UNNAMED' --add-modules 'java.desktop,java.sql,java.naming,java.net.http,java.xml,java.security.jgss,java.management,java.compiler,java.scripting,jdk.crypto.ec,jdk.unsupported'
if ($LASTEXITCODE -ne 0) { throw 'Pembuatan paket Windows gagal.' }
Copy-Item -LiteralPath 'config.example.properties' -Destination (Join-Path $output 'RetroQuest/config.example.properties')
Copy-Item -LiteralPath 'README.md' -Destination (Join-Path $output 'RetroQuest/README.md')
Copy-Item -LiteralPath 'docs' -Destination (Join-Path $output 'RetroQuest/docs') -Recurse
Copy-Item -LiteralPath 'examples' -Destination (Join-Path $output 'RetroQuest/examples') -Recurse
Write-Host "Paket siap: $output/RetroQuest/RetroQuest.exe"
