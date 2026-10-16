package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import viewpoint.packs.PackBind;
@Patch(className="viewpoint.world.Recipe",methodName="model",strictMatch=true)
public final class ProviderPackRecipeAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(0) Object recipe,@Patch.Argument(1) PackBind binding,@Patch.Argument(2) boolean turns,@Patch.Argument(3) float x,@Patch.Argument(4) float y,@Patch.Argument(5) float height,@Patch.Argument(6) int square,@Patch.Thrown Throwable failure){StorageProviders.packEmitted(recipe,binding,turns,x,y,height,square,failure==null);}
 private ProviderPackRecipeAdvice(){}
}
