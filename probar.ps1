$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& "$PSScriptRoot/compilar.ps1"
$jdkTaller = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin" } else { Split-Path (Get-Command javac -ErrorAction Stop).Source }
New-Item -ItemType Directory -Force target/test-classes | Out-Null
& (Join-Path $jdkTaller "javac.exe") -encoding UTF-8 -cp target/classes -d target/test-classes (Get-ChildItem src/test/java -Recurse -Filter *.java).FullName
if ($LASTEXITCODE -ne 0) { throw "No se pudieron compilar las pruebas." }
& (Join-Path $jdkTaller "java.exe") -cp "target/classes;target/test-classes;dist/lib/mssql-jdbc.jar" com.autotech.PruebasIntegracion
if ($LASTEXITCODE -ne 0) { throw "Fallaron las pruebas de integración." }
