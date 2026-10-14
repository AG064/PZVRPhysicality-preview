package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import viewpoint.packs.PackBind;
@Patch(className="viewpoint.world.Recipe",methodName="model",strictMatch=true)
public final class CounterTapRecipeAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(0) Object recipe,@Patch.Argument(1) PackBind binding,@Patch.Argument(2) boolean turns,@Patch.Thrown Throwable failure){CounterTapBindings.emitted(recipe,binding,turns,failure==null);}
 private CounterTapRecipeAdvice(){}
}
