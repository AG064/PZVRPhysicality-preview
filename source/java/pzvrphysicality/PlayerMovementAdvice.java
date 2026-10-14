package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="zombie.iso.IsoMovingObject",methodName="moveUnmoddedInternal",strictMatch=true)
public final class PlayerMovementAdvice {
 @Patch.OnEnter public static void enter(@Patch.This Object actor,@Patch.Argument(value=0,readOnly=false) float x,@Patch.Argument(value=1,readOnly=false) float y){if(RouteModes.playerCollision()){var motion=NativePlayerCollision.move(actor,x,y);x=motion[0];y=motion[1];}}
 private PlayerMovementAdvice(){}
}
