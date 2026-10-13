package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import viewpoint.packs.PackBind;
@Patch(className="viewpoint.world.Recipe",methodName="model",strictMatch=true)
public final class ShelfRecipeAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(1) PackBind binding,@Patch.Argument(2) boolean turns,@Patch.Thrown Throwable failure){if(failure==null)ShelfBridge.emitted(binding,turns);}
 private ShelfRecipeAdvice(){}
}
