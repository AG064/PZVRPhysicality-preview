package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.characters.IsoGameCharacter;
@Patch(className="local.pzvr.NativeRig",methodName="capture",strictMatch=true)
public final class FingerCaptureAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(2) IsoGameCharacter actor){FingerPoseDriver.begin(actor);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(){FingerPoseDriver.end();}
 private FingerCaptureAdvice(){}
}
