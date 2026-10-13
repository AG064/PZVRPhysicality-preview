package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import local.pzvr.XrBackend;import zombie.core.skinnedmodel.model.SkinningData;import org.joml.Matrix4f;
@Patch(className="local.pzvr.NativeRig",methodName="posture",strictMatch=true)
public final class FingerFrameAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(0) SkinningData skin,@Patch.Argument(1) Matrix4f[] source,@Patch.Argument(2) Matrix4f[] output,@Patch.Argument(6) XrBackend.Frame input){FingerPoseDriver.frame(skin,source,output,input);}
 private FingerFrameAdvice(){}
}
