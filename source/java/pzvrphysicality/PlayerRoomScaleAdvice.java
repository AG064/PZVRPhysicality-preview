package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="local.pzvr.RoomScale",methodName="before",strictMatch=true)
public final class PlayerRoomScaleAdvice {
 @Patch.OnEnter public static void enter(){PlayerCollisionScope.enter();}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(){PlayerCollisionScope.exit();}
 private PlayerRoomScaleAdvice(){}
}
