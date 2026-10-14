package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.iso.IsoChunk;
@Patch(className="viewpoint.world.WorldMesher",methodName="gather",strictMatch=true)
public final class OwnerVisibilityGatherAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(0) IsoChunk chunk,@Patch.Argument(1) int level,@Patch.Return Object recipe,@Patch.Thrown Throwable failure){NativeOwnerVisibility.gathered(chunk,level,recipe,failure==null);}
 private OwnerVisibilityGatherAdvice(){}
}
