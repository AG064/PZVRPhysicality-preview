package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.skinnedmodel.model.ModelMesh;
@Patch(className="zombie.core.skinnedmodel.model.ModelLoader",methodName="applyToMesh",strictMatch=true)
public final class ItemMeshCpuAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(0) Object loaded,@Patch.Argument(1) ModelMesh mesh){ItemMeshCpu.capture(loaded,mesh);BackpackMeshCpu.capture(loaded,mesh);}
 private ItemMeshCpuAdvice(){}
}
