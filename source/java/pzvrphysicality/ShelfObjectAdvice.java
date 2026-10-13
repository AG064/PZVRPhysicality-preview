package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.iso.IsoObject;
@Patch(className="pz.viewpointcontainers.ContainerStates",methodName="object",strictMatch=true)
public final class ShelfObjectAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(0) IsoObject owner){ShelfBridge.begin(owner);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Return boolean handled,@Patch.Thrown Throwable failure){ShelfBridge.end(handled&&failure==null);}
 private ShelfObjectAdvice(){}
}
