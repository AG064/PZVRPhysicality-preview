package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="viewpoint.world.FloorDecals",methodName="decal",strictMatch=true)
public final class MoveableIconAdvice {
 @Patch.OnEnter(skipOn=true) public static boolean enter(@Patch.Argument(2) float x,@Patch.Argument(3) float y,@Patch.Argument(4) float z){return MoveableModels.skipIcon(x,y,z);}
 private MoveableIconAdvice(){}
}
