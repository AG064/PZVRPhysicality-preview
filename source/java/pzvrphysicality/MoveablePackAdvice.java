package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import viewpoint.render.PackModel;import viewpoint.render.PackData;
@Patch(className="viewpoint.render.PackModel",methodName="read",strictMatch=true)
public final class MoveablePackAdvice {
 @Patch.OnExit public static void exit(@Patch.This PackModel model,@Patch.Argument(0) PackData data){if(Main.ready())MoveablePackCpu.capture(model,data);}
 private MoveablePackAdvice(){}
}
