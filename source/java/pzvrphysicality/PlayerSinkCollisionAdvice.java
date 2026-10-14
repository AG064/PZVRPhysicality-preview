package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="zombie.iso.IsoGridSquare",methodName="testCollideAdjacent",strictMatch=true)
public final class PlayerSinkCollisionAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.This Object square,@Patch.Argument(0) Object actor,@Patch.Argument(1) int dx,@Patch.Argument(2) int dy,@Patch.Argument(3) int dz,@Patch.Return(readOnly=false) boolean blocked,@Patch.Thrown Throwable failure){
  if(RouteModes.playerCollision())blocked=NativeSinkCollision.refine(square,actor,dx,dy,dz,blocked,failure==null);
 }
 private PlayerSinkCollisionAdvice(){}
}
