package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import viewpoint.core.Frame;
@Patch(className="viewpoint.models.Models",methodName="snapshot",strictMatch=true)
public final class HeldContactCaptureAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(0) Frame frame,@Patch.Thrown Throwable failure){if(failure==null)HeldContactDriver.capture(frame);}
 private HeldContactCaptureAdvice(){}
}
