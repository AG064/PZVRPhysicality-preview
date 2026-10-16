package pzvrphysicality;
import java.nio.file.*;
/** Cold composition and independent publisher crypto, never provider entry points. */
public final class SurfaceArtifactTest {
 public static void main(String[] args)throws Exception {
  Path out=Path.of(args[4]);Files.createDirectories(out);
  SurfaceCompositionContract.main(new String[]{args[1],args[0],args[2],args[3],args[4]});
  CompanionCompositionContract.main(args);
  LightReflectionContract.main(new String[]{args[0],args[1]});
  SurfaceReferenceContract.main(new String[]{args[2],args[0],args[1],out.resolve("reference-findings.txt").toString()});
  int referenceChecks=SurfaceReferenceContract.lastChecks;
  for(int index:new int[]{5,6}){SurfaceReferenceContract.main(new String[]{args[index],args[0],args[1],out.resolve("reference-findings-"+index+".txt").toString()});referenceChecks+=SurfaceReferenceContract.lastChecks;}
  SurfaceSignatureContract.main(new String[]{args[2],args[2]+".zbs","0a85a070765317a4d4d7c72ff048f0dcdca133f3f3f282c2c280cc73f01f1ea2"});
  int count=SurfaceCompositionContract.lastChecks+CompanionCompositionContract.lastChecks+LightReflectionContract.lastChecks+referenceChecks+SurfaceSignatureContract.lastChecks;
  Files.writeString(out.resolve("proof.json"),"{\"Passed\":true,\"Checks\":"+count+",\"ColdCompositionChecks\":"+SurfaceCompositionContract.lastChecks+",\"CurrentSurfaceAdviceClasses\":"+SurfaceCompositionContract.adviceClasses+",\"CurrentSurfaceAdviceTargets\":"+SurfaceCompositionContract.adviceTargets+",\"LightAdviceClasses\":"+CompanionCompositionContract.lightAdvice+",\"WeatherAdviceClasses\":"+CompanionCompositionContract.weatherAdvice+",\"LightReflectionChecks\":"+LightReflectionContract.lastChecks+",\"CompanionCompositionChecks\":"+CompanionCompositionContract.lastChecks+",\"SharedTargets\":"+CompanionCompositionContract.sharedTargets+",\"NativeReferenceChecks\":"+referenceChecks+",\"CryptoChecks\":"+SurfaceSignatureContract.lastChecks+",\"SignatureValid\":true,\"OriginalMethodsExecuted\":false}\n");
  System.out.println("Current companion contracts: "+count+" checks passed");
 }
}
