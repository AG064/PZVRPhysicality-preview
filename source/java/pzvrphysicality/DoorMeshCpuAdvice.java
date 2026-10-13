package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.skinnedmodel.model.ModelMesh;
/** Reads allowlisted original skinned door data before native GPU upload. */
@Patch(className="zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene",methodName="applyToMesh",strictMatch=true)
public final class DoorMeshCpuAdvice {
 @Patch.OnEnter public static void enter(@Patch.This Object scene,@Patch.Argument(0) ModelMesh mesh){DoorMeshCpu.capture(scene,mesh);}
 private DoorMeshCpuAdvice(){}
}
