package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.core.skinnedmodel.model.ModelMesh;
@Patch(className="zombie.core.skinnedmodel.model.ModelMesh",methodName="onLoadedX",strictMatch=true)
public final class UtensilMeshCpuAdvice {
 @Patch.OnEnter public static void enter(@Patch.This ModelMesh mesh,@Patch.Argument(0) Object scene){UtensilMeshCpu.capture(scene,mesh);BiteMeshCpu.capture(scene,mesh);BackpackMeshCpu.capture(scene,mesh);}
 private UtensilMeshCpuAdvice(){}
}
