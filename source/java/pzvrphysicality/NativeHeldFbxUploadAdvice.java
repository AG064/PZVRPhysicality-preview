package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.core.skinnedmodel.model.VertexBufferObject;
/** Observes the exact native queued static upload and its existing VBO. Never issues a graphics operation. */
@Patch(className="zombie.core.skinnedmodel.model.VertexBufferObject",methodName="LoadVBO",strictMatch=true)
public final class NativeHeldFbxUploadAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.This VertexBufferObject vbo,@Patch.Argument(0) VertexBufferObject.VertexArray vertices,@Patch.Argument(1) int[] faces,@Patch.Thrown Throwable failure){NativeHeldFbxCpu.uploaded(vbo,vertices,faces,failure);}
 private NativeHeldFbxUploadAdvice(){}
}
