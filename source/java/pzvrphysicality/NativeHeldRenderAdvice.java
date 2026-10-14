package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="viewpoint.models.Models",methodName="snapshot",strictMatch=true)
public final class NativeHeldRenderAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(0) Object frame,@Patch.Thrown Throwable failure){if(failure==null)NativeHeldFallback.render(frame);}
 private NativeHeldRenderAdvice(){}
}
