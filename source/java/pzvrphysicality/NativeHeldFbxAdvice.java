package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.core.skinnedmodel.model.ModelMesh;
@Patch(className="zombie.core.skinnedmodel.model.ModelMesh",methodName="onLoadedX",strictMatch=true)
public final class NativeHeldFbxAdvice {
 @Patch.OnEnter public static void enter(@Patch.This ModelMesh mesh,@Patch.Argument(0) Object scene){NativeHeldFbxCpu.begin(mesh,scene);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.This ModelMesh mesh,@Patch.Thrown Throwable failure){NativeHeldFbxCpu.end(mesh,failure);}
 private NativeHeldFbxAdvice(){}
}
