package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import viewpoint.world.TileMesh;import zombie.core.textures.Texture;
@Patch(className="viewpoint.world.WorldMesher",methodName="op",strictMatch=true)
public final class WindowMotionEmissionAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(1) int kind,@Patch.Argument(2) TileMesh mesh,@Patch.Argument(3) Texture texture,@Patch.Argument(4) float x,@Patch.Argument(5) float y,@Patch.Argument(6) float z,@Patch.Return Object op,@Patch.Thrown Throwable failure){if(failure==null)WindowMotion.emitted(op,mesh,texture,x,y,z,kind);}
 private WindowMotionEmissionAdvice(){}
}
