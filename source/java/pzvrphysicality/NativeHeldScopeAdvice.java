package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="local.pzvr.NativeRig",methodName="capture",strictMatch=true)
public final class NativeHeldScopeAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(0) Object frame,@Patch.Argument(1) Object slot,@Patch.Argument(2) Object actor){NativeHeldFallback.begin(frame,slot,actor);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Thrown Throwable failure){NativeHeldFallback.end(failure);}
 private NativeHeldScopeAdvice(){}
}
