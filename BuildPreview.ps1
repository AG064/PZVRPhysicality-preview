[CmdletBinding()]
param(
 [Parameter(Mandatory)][string]$JavaHome,
 [Parameter(Mandatory)][string]$GameJar,
 [Parameter(Mandatory)][string]$PzvrJar,
 [Parameter(Mandatory)][string]$ViewpointJar,
 [Parameter(Mandatory)][string]$ZombieBuddyJar,
 [Parameter(Mandatory)][string]$ShelvesJar,
 [Parameter(Mandatory)][string]$BetterWindowJar,
 [Parameter(Mandatory)][string]$PhysicsDll,
 [string]$TemporaryDirectory=[IO.Path]::GetTempPath()
)
$ErrorActionPreference='Stop'
$pins=@{
 GameJar='e1a69eb743ede60b213a0fe7f8b83d4fcab773036d256cc4543a336f3b058a33'
 PzvrJar='c44c165ca485d24211d1455e792a90b9ca2a44ee478c71a61240a7af4c348e3e'
 ViewpointJar='afa5829d16b3785b0e62673376da6d2fb4c16b0ba3966d9d4a4ea064f584edc8'
 ZombieBuddyJar='eb79b9876332010733a8e0d7ce4fe0377846059a9e28859029e2a0fb449f6cf2'
 ShelvesJar='cb111d813c3436b0355c44a65f9d38fb5ebb489a4497f10caaa2027240a116cf'
 BetterWindowJar='42f9aecafbb34a4bc76c6a8130720608fed79de9f78f179ef1fca9d4d635d6b1'
 PhysicsDll='f359aa66469119124fc3b51e4ed4b776bca294d1f4af0ea4a8e99e8cc342af7c'
}
foreach($key in $pins.Keys){$path=Get-Variable -Name $key -ValueOnly;if((Get-FileHash -LiteralPath $path).Hash.ToLowerInvariant() -ne $pins[$key]){throw "Unsupported build input: $key"}}
$compiler=Join-Path $JavaHome 'bin/javac.exe'
$version=& $compiler -version 2>&1
if($LASTEXITCODE -ne 0 -or ($version -join ' ') -notmatch '^javac 25(?:\.|\s|$)'){throw 'JDK 25 is required for the pinned game JRE'}
$run=Join-Path ([IO.Path]::GetFullPath($TemporaryDirectory)) ('pzvr-source-build-'+[Guid]::NewGuid().ToString('N'))
$classes=Join-Path $run 'classes';$payload=Join-Path $run 'payload/PZVRPhysicality'
New-Item -ItemType Directory -Path $classes,$payload|Out-Null
$sourceRoot=Join-Path $PSScriptRoot 'source'
$sources=@(Get-ChildItem -LiteralPath (Join-Path $sourceRoot 'java') -Recurse -File -Filter '*.java'|Select-Object -ExpandProperty FullName)
if($sources.Count -lt 20){throw 'Public source tree is missing'}
& $compiler --release 25 -Xlint:all -cp "$GameJar;$PzvrJar;$ViewpointJar;$ZombieBuddyJar" -d $classes @sources
if($LASTEXITCODE -ne 0){throw 'Preview source compilation failed'}
$noticeTarget=Join-Path $classes 'META-INF/licenses';New-Item -ItemType Directory -Path $noticeTarget -Force|Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'LICENSE') -Destination (Join-Path $classes 'META-INF/LICENSE')
Copy-Item -LiteralPath (Join-Path $sourceRoot 'licenses/rust-physics') -Destination $noticeTarget -Recurse
Copy-Item -LiteralPath (Join-Path $sourceRoot 'mod/42') -Destination $payload -Recurse
$jarDir=Join-Path $payload '42/media/java/client';New-Item -ItemType Directory -Path (Join-Path $jarDir 'native') -Force|Out-Null
& (Join-Path $JavaHome 'bin/jar.exe') --create --file (Join-Path $jarDir 'PZVRPhysicality.jar') -C $classes .
if($LASTEXITCODE -ne 0){throw 'Preview JAR creation failed'}
Copy-Item -LiteralPath $PhysicsDll -Destination (Join-Path $jarDir 'native/pzvr_physics.dll')
foreach($name in @('README.md','LICENSE','LIVE-TEST-CHECKLIST.md','DEPENDENCIES.md')){Copy-Item -LiteralPath (Join-Path $PSScriptRoot $name) -Destination (Join-Path $payload $name)}
$archive=Join-Path $run 'PZVRPhysicality-source-build.zip'
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::CreateFromDirectory((Join-Path $run 'payload'),$archive)
[pscustomobject]@{Archive=$archive;SHA256=(Get-FileHash -LiteralPath $archive).Hash.ToLowerInvariant();SDKCompilePassed=$true;GameExecuted=$false;HeadsetVerified=$false;ReleaseRegressionGateReused=$false}|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $run 'source-build-readback.json')
Write-Output "Compiled personal preview: $archive"
Write-Output 'This source build has compilation proof only. The published release carries the separate regression gate and still needs live headset acceptance.'
