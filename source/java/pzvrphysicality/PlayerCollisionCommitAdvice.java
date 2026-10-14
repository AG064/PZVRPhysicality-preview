package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="zombie.iso.IsoMovingObject",methodName="postupdate",strictMatch=true)
public final class PlayerCollisionCommitAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.This Object actor,@Patch.Thrown Throwable failure){if(RouteModes.playerCollision())NativePlayerCollision.observed(actor,failure==null);}
 private PlayerCollisionCommitAdvice(){}
}
