$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$jdkTaller = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin" } else { Split-Path (Get-Command javac -ErrorAction Stop).Source }
$driverTaller = Join-Path $env:USERPROFILE ".m2\repository\com\microsoft\sqlserver\mssql-jdbc\12.10.1.jre11\mssql-jdbc-12.10.1.jre11.jar"
if (!(Test-Path -LiteralPath $driverTaller)) { throw "Abre y compila primero el proyecto Maven en NetBeans para descargar el driver JDBC." }
New-Item -ItemType Directory -Force target/classes, dist/lib | Out-Null
$fuentesTaller = (Get-ChildItem src/main/java -Recurse -Filter *.java).FullName
& (Join-Path $jdkTaller "javac.exe") -encoding UTF-8 -d target/classes $fuentesTaller
if ($LASTEXITCODE -ne 0) { throw "No se pudo compilar." }
Copy-Item -LiteralPath $driverTaller -Destination dist/lib/mssql-jdbc.jar -Force
"Manifest-Version: 1.0`nMain-Class: com.autotech.AutoTechManager`nClass-Path: lib/mssql-jdbc.jar`n`n" | Set-Content target/MANIFEST.MF -Encoding ascii
& (Join-Path $jdkTaller "jar.exe") --create --file dist/AutoTechManager.jar --manifest target/MANIFEST.MF -C target/classes .
if ($LASTEXITCODE -ne 0) { throw "No se pudo crear el ejecutable." }
Write-Host "Listo: dist/AutoTechManager.jar. Ejecuta iniciar.bat."
