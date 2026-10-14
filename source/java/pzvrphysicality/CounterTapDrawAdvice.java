package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import viewpoint.render.SceneData;
@Patch(className="viewpoint.render.PackDraws",methodName="collect",strictMatch=true)
public final class CounterTapDrawAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(0) SceneData scene,@Patch.Argument(1) int index,@Patch.Argument(2) long mask,@Patch.Thrown Throwable failure){CounterTapBindings.collected(scene,index,mask,failure==null);}
 private CounterTapDrawAdvice(){}
}
