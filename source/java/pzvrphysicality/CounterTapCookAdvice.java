package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import viewpoint.render.ChunkMeshData;
@Patch(className="viewpoint.world.Cook",methodName="mesh",strictMatch=true)
public final class CounterTapCookAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(0) Object recipe,@Patch.Return ChunkMeshData data,@Patch.Thrown Throwable failure){CounterTapBindings.cooked(recipe,data,failure==null);}
 private CounterTapCookAdvice(){}
}
