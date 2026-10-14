package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import viewpoint.render.SceneData;
@Patch(className="viewpoint.render.WorldRenderer",methodName="finish",strictMatch=true)
public final class MoveableRenderAdvice {
 @Patch.OnExit public static void exit(@Patch.Argument(0) SceneData scene){AuthoredWorldPass.draw(scene);}
 private MoveableRenderAdvice(){}
}
