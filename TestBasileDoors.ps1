param(
 [Parameter(Mandatory)][string]$JavaHome,[Parameter(Mandatory)][string]$GameJar,
 [Parameter(Mandatory)][string]$PzvrJar,[Parameter(Mandatory)][string]$ViewpointJar,
 [Parameter(Mandatory)][string]$ZombieBuddyJar,[Parameter(Mandatory)][string]$BaselineClasses,
 [Parameter(Mandatory)][string]$BasileDoorsDirectory,[Parameter(Mandatory)][string]$RunRoot,
 [string]$SharedFixtureRoot=$PSScriptRoot,[switch]$FinalGateMode
)
$ErrorActionPreference='Stop'
$run=[IO.Path]::GetFullPath($RunRoot)
$policy=Join-Path $PSScriptRoot 'Build-Temporary-Policy.ps1'
if(Test-Path -LiteralPath $policy){
 . $policy
 if(!(Test-PhysicalityTemporaryPath $run)){throw 'Fresh trusted ephemeral Basile output required'}
 Assert-PhysicalityBuildReserve $run
}else{
 $temp=[IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\')+'\'
 if(!$run.StartsWith($temp,[StringComparison]::OrdinalIgnoreCase)){throw 'Fresh portable temporary Basile output required'}
 $ancestor=$run;while($ancestor){if((Test-Path -LiteralPath $ancestor)-and((Get-Item -LiteralPath $ancestor).Attributes-band[IO.FileAttributes]::ReparsePoint)){throw 'Temporary reparse path rejected'};$ancestor=Split-Path $ancestor -Parent}
 if(([IO.DriveInfo]::new([IO.Path]::GetPathRoot($run))).AvailableFreeSpace-lt1GB){throw 'One GiB build reserve required'}
}
if((Test-Path -LiteralPath $run)-or$run.StartsWith($PSScriptRoot.TrimEnd('\')+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Fresh separate Basile output required'}
$sourceRoot=Join-Path $PSScriptRoot 'src';$verificationRoot=$PSScriptRoot
if(!(Test-Path -LiteralPath $sourceRoot)){
 $sourceRoot=Join-Path $PSScriptRoot 'source/java';$verificationRoot=Join-Path $PSScriptRoot 'source/verification'
 if($SharedFixtureRoot-eq$PSScriptRoot){$SharedFixtureRoot=$verificationRoot}
}
New-Item -ItemType Directory -Path $run|Out-Null
$classes=Join-Path $run 'classes';$fixtures=Join-Path $run 'fixtures';$tests=Join-Path $run 'tests'
foreach($path in @($classes,$fixtures,$tests)){New-Item -ItemType Directory -Path $path|Out-Null}
$sources=@('BasileDoorAssets','BasileDoorCapture','DoorMeshCpu','NativeDoorCapture','GroundItemAdvice')|ForEach-Object{Join-Path $sourceRoot ('pzvrphysicality/'+$_+'.java')}
$map=@{}
foreach($folder in @((Join-Path $SharedFixtureRoot 'world-fixtures'),(Join-Path $SharedFixtureRoot 'door-fixtures'),(Join-Path $verificationRoot 'basile-door-fixtures'))){
 foreach($file in Get-ChildItem -LiteralPath $folder -Recurse -File -Filter '*.java'){$map[[IO.Path]::GetRelativePath($folder,$file.FullName)]=$file.FullName}
}
$fixtureSources=@($map.Values|Sort-Object)
$testSources=@((Join-Path $SharedFixtureRoot 'door-test/pzvrphysicality/DoorMeshCpuTest.java'),(Join-Path $SharedFixtureRoot 'door-test/pzvrphysicality/NativeDoorCaptureTest.java'))
if(!$FinalGateMode){$testSources+=Join-Path $SharedFixtureRoot 'door-test/pzvrphysicality/NativeDoorPackedFallbackTest.java'}
$testSources+=@(Get-ChildItem -LiteralPath (Join-Path $verificationRoot 'basile-door-test') -Recurse -File -Filter '*.java'|ForEach-Object FullName)
$jar=Join-Path $BasileDoorsDirectory '42/media/java/client/BasileDoors3D.jar'
$inputs=@{}
foreach($file in @($sources+$fixtureSources+$testSources+@($PSCommandPath,(Join-Path $PSScriptRoot 'prepare_basile_fixtures.py'),$GameJar,$PzvrJar,$ViewpointJar,$ZombieBuddyJar,$jar))){$inputs[$file]=(Get-FileHash -LiteralPath $file).Hash.ToLowerInvariant()}
foreach($key in @('garage_white_garage_w_0_closed','garage_white_garage_w_0_open','carpentry_carpentry_01_48')){$file=Join-Path $BasileDoorsDirectory ('42/media/models_X/BasileDoors3D/'+$key+'.glb');$inputs[$file]=(Get-FileHash -LiteralPath $file).Hash.ToLowerInvariant()}
foreach($localPolicy in @($policy,(Join-Path $PSScriptRoot 'Build-Temporary-Policy.json'))){if(Test-Path -LiteralPath $localPolicy){$inputs[$localPolicy]=(Get-FileHash -LiteralPath $localPolicy).Hash.ToLowerInvariant()}}
$baselineHashes=@{}
foreach($file in Get-ChildItem -LiteralPath $BaselineClasses -Recurse -File -Filter '*.class'){$baselineHashes[$file.FullName]=(Get-FileHash -LiteralPath $file.FullName).Hash.ToLowerInvariant()}
$numeric=Join-Path $run 'private-numeric'
& python (Join-Path $PSScriptRoot 'prepare_basile_fixtures.py') --mod-directory $BasileDoorsDirectory --out $numeric *> (Join-Path $run 'asset-preparation.log')
if($LASTEXITCODE){throw 'Pinned private Basile asset preparation failed'}
$sdk="$GameJar;$PzvrJar;$ViewpointJar;$ZombieBuddyJar;$BaselineClasses"
& "$JavaHome/bin/javac.exe" -J-Xmx128m -Xlint:all -sourcepath $classes -cp $sdk -d $classes @sources *> (Join-Path $run 'sdk-compile.log')
if($LASTEXITCODE){throw 'Basile actual SDK-first compilation failed'}
$cp="$classes;$BaselineClasses;$GameJar;$ZombieBuddyJar"
& "$JavaHome/bin/javac.exe" -J-Xmx128m -Xlint:all -cp $cp -d $fixtures @fixtureSources *> (Join-Path $run 'fixture-compile.log')
if($LASTEXITCODE){throw 'Basile authored fixture compilation failed'}
& "$JavaHome/bin/javac.exe" -J-Xmx128m -Xlint:all -cp "$fixtures;$cp" -d $tests @testSources *> (Join-Path $run 'test-compile.log')
if($LASTEXITCODE){throw 'Basile authored test compilation failed'}
$cases=@(
 @{Name='BasileDoorCaptureTest';Args=@($jar,(Join-Path $numeric 'garage-closed.bin'),$run);New=$true},
 @{Name='BasileNativeClockTest';Args=@($jar,(Join-Path $numeric 'native-leaf.bin'),$run);New=$true},
 @{Name='BasileGarageOpeningTest';Args=@($jar,(Join-Path $numeric 'garage-closed.bin'),(Join-Path $numeric 'garage-open.bin'),$run);New=$true}
)
if(!$FinalGateMode){$cases+=@(@{Name='DoorMeshCpuTest';Args=@();New=$false},@{Name='NativeDoorCaptureTest';Args=@($GameJar,$ViewpointJar);New=$false},@{Name='NativeDoorPackedFallbackTest';Args=@();New=$false})}
$counts=@{};$newCount=0;$retained=0;$origins=@{}
Push-Location $run
try{foreach($case in $cases){
 $name=$case.Name;$log=Join-Path $run ($name+'-origin.log');$result=Join-Path $run ($name+'-result.txt')
 & "$JavaHome/bin/java.exe" -Xmx128m -Xverify:all -ea ('-Xlog:class+load=info:file='+$log) -cp "$fixtures;$classes;$BaselineClasses;$tests;$GameJar;$ZombieBuddyJar" ('pzvrphysicality.'+$name) @($case.Args) *> $result
 $exit=$LASTEXITCODE
 $foreign=@(Get-Content -LiteralPath $log|Where-Object{$_-match '\] (zombie[./]|viewpoint[./]|local[./]pzvr[./]|basile[./])'-and$_-match 'source: .*\.jar'})
 $origins[$name]=@{OriginalNamespaceLoads=$foreign.Count;ExitCode=$exit;LogSHA256=(Get-FileHash -LiteralPath $log).Hash.ToLowerInvariant();ResultSHA256=(Get-FileHash -LiteralPath $result).Hash.ToLowerInvariant()}
 if($foreign.Count){throw ('Original namespace defined: '+$name)}
 if($exit){Get-Content -LiteralPath $result -Tail 12;throw ('Basile test failed: '+$name)}
 $output=Get-Content -LiteralPath $result -Raw
 if($output-notmatch ': (\d+) checks passed'){throw ('Missing exact Basile counter: '+$name)}
 $count=[int]$Matches[1];$counts[$name]=$count;if($case.New){$newCount+=$count}else{$retained+=$count}
 ($output-split "`n"|Where-Object{$_-match 'checks passed'})|Write-Output
}}finally{Pop-Location}
foreach($row in $inputs.GetEnumerator()){if((Get-FileHash -LiteralPath $row.Key).Hash.ToLowerInvariant()-ne$row.Value){throw ('Basile source/SDK/asset input drift: '+$row.Key)}}
foreach($row in $baselineHashes.GetEnumerator()){if((Get-FileHash -LiteralPath $row.Key).Hash.ToLowerInvariant()-ne$row.Value){throw ('Baseline class drift: '+$row.Key)}}
function ClassHashes([string]$root){$values=@{};foreach($file in Get-ChildItem -LiteralPath $root -Recurse -File -Filter '*.class'){$values[[IO.Path]::GetRelativePath($root,$file.FullName)]=(Get-FileHash -LiteralPath $file.FullName).Hash.ToLowerInvariant()};return $values}
[ordered]@{Passed=$true;Checks=$newCount;Counts=$counts;RetainedNativeDoorChecks=$retained;RetainedCountedAgain=$false;FinalGateMode=[bool]$FinalGateMode;Inputs=$inputs;BaselineClassHashes=$baselineHashes;ClassHashes=(ClassHashes $classes);FixtureClassHashes=(ClassHashes $fixtures);TestClassHashes=(ClassHashes $tests);Origins=$origins;Classes=$classes;ActualSDKCompileFirst=$true;OriginalNamespaceLoads=0;OriginalMethodsExecuted=$false;GameExecuted=$false;HeadsetVerified=$false;GPUExecuted=$false;MaximumHeapMiB=128;ProviderAssetsPackaged=$false}|ConvertTo-Json -Depth 8|Set-Content -LiteralPath (Join-Path $run 'readback.json')
