package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import org.joml.Matrix4f;
@Patch(className="local.pzvr.NativeRig",methodName="fingers",strictMatch=true)
public final class NativeHeldPoseAdvice {
 @Patch.OnExit public static void exit(@Patch.Argument(0) Object skin,@Patch.Argument(2) Matrix4f[] output){NativeHeldFallback.fingers(skin,output);}
 private NativeHeldPoseAdvice(){}
}
