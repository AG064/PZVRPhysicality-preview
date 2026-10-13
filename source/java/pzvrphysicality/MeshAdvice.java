package pzvrphysicality;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.render.PackModel;
import viewpoint.render.PackData;

/** Copies completed CPU mesh data. No rendering, upload, residency or texture calls are replaced. */
@Patch(className="viewpoint.render.PackModel",methodName="read",strictMatch=true)
public final class MeshAdvice {
 @Patch.OnExit public static void exit(@Patch.This PackModel model,@Patch.Argument(0) PackData data){
  if(Main.ready())viewpointvr.physics.PackedModelGeometry.capture(model,data);
 }
 private MeshAdvice(){}
}
