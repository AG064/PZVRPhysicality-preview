package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.iso.IsoObject;
@Patch(className="viewpoint.world.PackGather",methodName="object",strictMatch=true)
public final class ProviderPackObjectAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(0) IsoObject owner,@Patch.Argument(2) float x,@Patch.Argument(3) float y,@Patch.Argument(4) float height,@Patch.Argument(5) int square){StorageProviders.packBegin(owner,x,y,height,square);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Return boolean handled,@Patch.Thrown Throwable failure){StorageProviders.packEnd(handled&&failure==null);}
 private ProviderPackObjectAdvice(){}
}
