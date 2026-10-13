package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import local.pzvr.XrBackend;import org.lwjgl.system.MemoryStack;
@Patch(className="local.pzvr.OpenXrBackend",methodName="readHands",strictMatch=true)
public final class ControllerTouchReadAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.This Object backend,@Patch.Argument(0) MemoryStack stack,@Patch.Argument(1) long time,@Patch.Return XrBackend.Hand[] hands,@Patch.Thrown Throwable failure){if(failure==null)ControllerTouch.capture(backend,stack,time,hands);else ControllerTouch.close(backend);}
 private ControllerTouchReadAdvice(){}
}
