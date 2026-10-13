package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.core.skinnedmodel.model.SkinningData;import org.joml.Matrix4f;
@Patch(className="local.pzvr.NativeRig",methodName="fingers",strictMatch=true)
public final class FingerPoseAdvice {
 @Patch.OnExit public static void exit(@Patch.Argument(0) SkinningData skin,@Patch.Argument(1) Matrix4f[] source,@Patch.Argument(2) Matrix4f[] output,@Patch.Argument(3) int side,@Patch.Argument(4) float nativeAmount,@Patch.Argument(5) Matrix4f[] reference){FingerPoseDriver.pose(skin,source,output,side,nativeAmount,reference);}
 private FingerPoseAdvice(){}
}
