package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import viewpoint.render.SceneData;
@Patch(className="viewpoint.render.PackDraws",methodName="draw",strictMatch=true)
public final class CounterTapDrawScopeAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(0) SceneData scene,@Patch.Argument(2) byte flags){CounterTapBindings.beginDraw(scene,flags);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Thrown Throwable failure){CounterTapBindings.endDraw(failure==null);}
 private CounterTapDrawScopeAdvice(){}
}
