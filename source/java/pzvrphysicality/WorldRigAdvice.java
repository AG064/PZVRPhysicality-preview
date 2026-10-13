package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import local.pzvr.XrBackend;
@Patch(className="local.pzvr.RigContacts",methodName="capture",strictMatch=true)
public final class WorldRigAdvice {
 @Patch.OnExit public static void exit(@Patch.Argument(0) Object frame,@Patch.Argument(1) Object actor,@Patch.Argument(2) XrBackend.Frame input){WorldRigCapture.capture(frame,actor,input);}
 private WorldRigAdvice(){}
}
