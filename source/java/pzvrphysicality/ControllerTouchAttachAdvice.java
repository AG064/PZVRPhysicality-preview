package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
/** Read-only completion receipt for the original create/suggest/attach lifecycle. */
@Patch(className="local.pzvr.OpenXrBackend",methodName="createActions",strictMatch=true)
public final class ControllerTouchAttachAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.This Object backend,@Patch.Thrown Throwable failure){ControllerTouch.actionsCreated(backend,failure);}
 private ControllerTouchAttachAdvice(){}
}
