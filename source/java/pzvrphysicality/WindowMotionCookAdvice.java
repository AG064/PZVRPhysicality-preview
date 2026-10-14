package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="viewpoint.world.Cook$Cooker",methodName="run",strictMatch=true)
public final class WindowMotionCookAdvice {
 @Patch.OnEnter public static void enter(@Patch.This Object cooker,@Patch.Argument(0) Object op,@Patch.Local("windowFirst") int first){first=WindowMotion.cookBegin(cooker,op);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.This Object cooker,@Patch.Argument(0) Object op,@Patch.Local("windowFirst") int first,@Patch.Thrown Throwable failure){WindowMotion.cookEnd(cooker,op,first,failure==null);}
 private WindowMotionCookAdvice(){}
}
