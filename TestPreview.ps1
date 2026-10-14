param([Parameter(Mandatory)][string]$JavaHome,[Parameter(Mandatory)][string]$GameJar,[Parameter(Mandatory)][string]$Classes,[string]$TemporaryDirectory=[IO.Path]::GetTempPath())
$ErrorActionPreference='Stop'
if((Get-FileHash -LiteralPath $GameJar).Hash.ToLowerInvariant()-ne'e1a69eb743ede60b213a0fe7f8b83d4fcab773036d256cc4543a336f3b058a33'){throw 'Unsupported math/runtime SDK'}
$run=Join-Path ([IO.Path]::GetFullPath($TemporaryDirectory)) ('pzvr-authored-policy-'+[guid]::NewGuid().ToString('N'))
if(Test-Path -LiteralPath $run){throw 'Fresh temporary policy-test directory required'}
New-Item -ItemType Directory -Path $run|Out-Null
$sourceRoot=Join-Path $PSScriptRoot 'source\tests';if(!(Test-Path $sourceRoot)){$sourceRoot=Join-Path $PSScriptRoot 'test'}
$names=@('GripRouteTest','FingerPoseTest','PlayerSweepTest');$files=$names|ForEach-Object{Join-Path $sourceRoot ('pzvrphysicality\'+$_+'.java')}
& "$JavaHome/bin/javac.exe" -J-Xmx128m -Xlint:all -cp "$Classes;$GameJar" -d $run @files *> (Join-Path $run 'compile.log');if($LASTEXITCODE){throw 'Authored policy compile failed'}
$counts=@{}
foreach($name in $names){$log=Join-Path $run ($name+'-origin.log');$output=& "$JavaHome/bin/java.exe" -Xmx128m -Xverify:all -ea "-Xlog:class+load=info:file=$log" -cp "$run;$Classes;$GameJar" ('pzvrphysicality.'+$name) 2>&1;$output|Set-Content (Join-Path $run ($name+'-result.txt'));if($LASTEXITCODE){throw ('Authored policy regression: '+$name)};if(($output-join"`n")-notmatch': (\d+) [^\r\n]*passed'){throw 'Missing policy count'};$counts[$name]=[int]$Matches[1];$output|Select-Object -Last 1|Write-Output;if(Get-Content $log|Where-Object{$_-match'(zombie[./]|viewpoint[./]|local[./]pzvr[./]).*source:.*\.jar'}){throw 'Original game/mod namespace loaded by pure policy test'}}
@{Passed=$true;Checks=($counts.Values|Measure-Object -Sum).Sum;Counts=$counts;SubsetOnly=$true;PrivateAssetsRequired=$false;GameExecuted=$false;HeadsetVerified=$false;Output=$run}|ConvertTo-Json -Depth 4|Set-Content (Join-Path $run 'readback.json')
Write-Output (Join-Path $run 'readback.json')
