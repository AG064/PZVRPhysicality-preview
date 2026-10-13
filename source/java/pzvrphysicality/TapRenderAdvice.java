package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import viewpoint.render.SceneData;
@Patch(className="viewpoint.render.WorldRenderer",methodName="finish",strictMatch=true)
public final class TapRenderAdvice {
 @Patch.OnExit public static void exit(@Patch.Argument(0) SceneData scene){TapRenderHook.draw(scene);BiteRenderHook.draw(scene);}
 private TapRenderAdvice(){}
}
