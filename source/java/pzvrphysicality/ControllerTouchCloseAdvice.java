package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="local.pzvr.OpenXrBackend",methodName="close",strictMatch=true)
public final class ControllerTouchCloseAdvice {
 @Patch.OnEnter public static void enter(@Patch.This Object backend){ControllerTouch.close(backend);}
 private ControllerTouchCloseAdvice(){}
}
