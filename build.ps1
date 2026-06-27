# Tank Battle build script (PowerShell): compile all .java under src and package as TankBattle.jar
Set-Location -Path $PSScriptRoot
if (-not (Test-Path out)) { New-Item -ItemType Directory -Path out | Out-Null }

Write-Host "[1/3] Compiling sources..."
javac -encoding UTF-8 -d out src/*.java
if ($LASTEXITCODE -ne 0) { Write-Host "Compile failed."; exit 1 }

Write-Host "[2/3] Packaging jar..."
jar cfm TankBattle.jar Manifest.txt -C out .
if ($LASTEXITCODE -ne 0) { Write-Host "Package failed."; exit 1 }

Write-Host "[3/3] Done. Run with:  java -jar TankBattle.jar"
