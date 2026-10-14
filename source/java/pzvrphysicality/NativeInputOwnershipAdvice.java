package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
/** Ownership telemetry only, after the native input filter/grab update. */
@Patch(className="local.pzvr.InputBridge",methodName="update",strictMatch=true)
public final class NativeInputOwnershipAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Thrown Throwable failure){NativeInputOwnership.observe(failure);}
 private NativeInputOwnershipAdvice(){}
}
