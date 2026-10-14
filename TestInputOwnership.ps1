param([Parameter(Mandatory)][string]$JavaHome,[Parameter(Mandatory)][string]$GameJar,[Parameter(Mandatory)][string]$PzvrJar,[Parameter(Mandatory)][string]$ViewpointJar,[Parameter(Mandatory)][string]$ZombieBuddyJar,[Parameter(Mandatory)][string]$Classes,[Parameter(Mandatory)][string]$RunRoot)
$ErrorActionPreference='Stop';$run=[IO.Path]::GetFullPath($RunRoot);$policy=Join-Path $PSScriptRoot 'Build-Temporary-Policy.ps1'
if(Test-Path -LiteralPath $policy){. $policy;$valid=Test-PhysicalityTemporaryPath $run}else{$temp=[IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\')+'\';$valid=$run.StartsWith($temp,[StringComparison]::OrdinalIgnoreCase);$ancestor=$run;while($ancestor){if((Test-Path -LiteralPath $ancestor)-and((Get-Item -LiteralPath $ancestor).Attributes-band[IO.FileAttributes]::ReparsePoint)){throw 'Temporary reparse path rejected'};$ancestor=Split-Path $ancestor -Parent}}
if(!$valid-or(Test-Path -LiteralPath $run)){throw 'Fresh ephemeral input ownership verification required'}
$sourceRoot=Join-Path $PSScriptRoot 'src';$verificationRoot=$PSScriptRoot
if(!(Test-Path -LiteralPath $sourceRoot)){$sourceRoot=Join-Path $PSScriptRoot 'source/java';$verificationRoot=Join-Path $PSScriptRoot 'source/verification'}
$pins=@{GameJar='e1a69eb743ede60b213a0fe7f8b83d4fcab773036d256cc4543a336f3b058a33';PzvrJar='c44c165ca485d24211d1455e792a90b9ca2a44ee478c71a61240a7af4c348e3e';ViewpointJar='afa5829d16b3785b0e62673376da6d2fb4c16b0ba3966d9d4a4ea064f584edc8';ZombieBuddyJar='eb79b9876332010733a8e0d7ce4fe0377846059a9e28859029e2a0fb449f6cf2'}
foreach($name in $pins.Keys){if((Get-FileHash -LiteralPath (Get-Variable $name -ValueOnly)).Hash.ToLowerInvariant()-ne$pins[$name]){throw ('Unsupported input ownership SDK: '+$name)}}
New-Item -ItemType Directory -Path $run|Out-Null
$c=Join-Path $run 'classes';$f=Join-Path $run 'fixtures';$t=Join-Path $run 'tests';New-Item -ItemType Directory -Path $c,$f,$t|Out-Null
$sdk="$GameJar;$PzvrJar;$ViewpointJar;$ZombieBuddyJar"
$sources=@('NativeInputOwnership','NativeInputOwnershipAdvice','PzvrContacts','FingerPoseDriver','FingerCaptureAdvice','FingerFrameAdvice','FingerPoseAdvice','ControllerTouch','ControllerTouchSuggestAdvice','ControllerTouchReadAdvice','ControllerTouchCloseAdvice','ControllerTouchAttachAdvice','GripRoute','ConsumeRoute')|ForEach-Object{Join-Path $sourceRoot ('pzvrphysicality/'+$_+'.java')}
$fixtureSources=@(Get-ChildItem (Join-Path $verificationRoot 'controller-touch-fixtures') -Recurse -File -Filter '*.java'|Where-Object{$_.Name-ne'Engine.java'}|ForEach-Object FullName)
$fixtureSources+=@(Get-ChildItem (Join-Path $verificationRoot 'input-ownership-fixtures') -Recurse -File -Filter '*.java'|ForEach-Object FullName)
$fixtureSources+=@('Math3.java','XrBackend.java')|ForEach-Object{Join-Path $verificationRoot ('fixtures/local/pzvr/'+$_)}
$testSources=@(Get-ChildItem (Join-Path $verificationRoot 'controller-touch-test') -Recurse -File -Filter '*.java'|ForEach-Object FullName)
$testSources+=@(Get-ChildItem (Join-Path $verificationRoot 'input-ownership-test') -Recurse -File -Filter '*.java'|ForEach-Object FullName)
$inputs=@{};foreach($path in $sources+$fixtureSources+$testSources+@($PSCommandPath,$GameJar,$PzvrJar,$ViewpointJar,$ZombieBuddyJar)){$inputs[$path]=(Get-FileHash -LiteralPath $path).Hash.ToLowerInvariant()}
function Compile([string]$Name,[string]$CP,[string]$Out,[string[]]$Files){& "$JavaHome/bin/javac.exe" -J-Xmx192m -Xlint:all -cp $CP -d $Out @Files *> (Join-Path $run ($Name+'-compile.log'));if($LASTEXITCODE){throw ('Input ownership compile failed: '+$Name)}}
Compile 'actual-sdk' "$sdk;$Classes" $c $sources
Compile 'authored-fixtures' "$c;$Classes;$GameJar;$ZombieBuddyJar" $f $fixtureSources
Compile 'tests' "$f;$c;$Classes;$GameJar;$ZombieBuddyJar" $t $testSources
$counts=@{}
foreach($name in @('NativeInputOwnershipTest','ControllerTouchTest','FingerPoseIntegrationTest','NativeInputCompositionTest')){
 $cp="$f;$c;$Classes;$t;$GameJar;$ZombieBuddyJar";$arguments=@()
 if($name-eq'NativeInputCompositionTest'){$cp="$t;$GameJar;$ZombieBuddyJar";$arguments=@($PzvrJar,$GameJar,$ViewpointJar,$ZombieBuddyJar,$c,(Join-Path $run 'copied'),$Classes)}
 $log=Join-Path $run ($name+'-origin.log');$output=& "$JavaHome/bin/java.exe" -Xmx128m -Xverify:all -ea "-Xlog:class+load=info:file=$log" -cp $cp ('pzvrphysicality.'+$name) @arguments 2>&1
 $output|Set-Content (Join-Path $run ($name+'-result.txt'));if($LASTEXITCODE){throw ('Input ownership regression failed: '+$name)}
 if(($output-join"`n")-notmatch': (\d+) checks passed'){throw 'Missing input ownership counter'};$counts[$name]=[int]$Matches[1];$output|Select-Object -Last 1|Write-Output
 if(Get-Content $log|Where-Object{$_-match'\] (?:zombie\.|local\.pzvr\.|viewpoint\.)'-and$_-match'source: .*\.jar'}){throw 'Original game/mod namespace loaded in input ownership runtime'}
}
foreach($entry in $inputs.GetEnumerator()){if((Get-FileHash -LiteralPath $entry.Key).Hash.ToLowerInvariant()-ne$entry.Value){throw 'Input ownership source changed during gate'}}
$hashes=@{};foreach($file in Get-ChildItem $c -Recurse -File -Filter '*.class'){$hashes[[IO.Path]::GetRelativePath($c,$file.FullName)]=(Get-FileHash -LiteralPath $file.FullName).Hash.ToLowerInvariant()}
@{Passed=$true;Checks=($counts.Values|Measure-Object -Sum).Sum;Counts=$counts;ControllerTouchChecks=($counts.ControllerTouchTest+$counts.FingerPoseIntegrationTest);InputOwnershipChecks=($counts.NativeInputOwnershipTest+$counts.NativeInputCompositionTest);Inputs=$inputs;ClassHashes=$hashes;SDKCompiledFirst=$true;OriginalGameModLoads=0;OriginalGameMethodsExecuted=$false;GPUExecuted=$false;HeadsetVerified=$false;CopiedSdkDefinitionsNeverInitialized=$true}|ConvertTo-Json -Depth 6|Set-Content (Join-Path $run 'readback.json')
