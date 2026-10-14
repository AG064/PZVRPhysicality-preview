package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.iso.IsoGridSquare;
@Patch(className="viewpoint.world.FloorDecals",methodName="items",strictMatch=true)
public final class MoveableFloorAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(0) IsoGridSquare square,@Patch.Argument(1) float x,@Patch.Argument(2) float y,@Patch.Argument(3) float z,@Patch.Argument(4) boolean[] pending){MoveableModels.begin(square,x,y,z,pending);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Thrown Throwable failure){MoveableModels.end(failure==null);}
 private MoveableFloorAdvice(){}
}
