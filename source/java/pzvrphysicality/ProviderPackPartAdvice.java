package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.iso.sprite.IsoSprite;
@Patch(className="viewpoint.world.PackGather",methodName="part",strictMatch=true)
public final class ProviderPackPartAdvice {
 @Patch.OnEnter(skipOn=true) public static boolean enter(@Patch.Argument(0) IsoSprite sprite){return StorageProviders.packPart(sprite);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Return(readOnly=false) boolean handled,@Patch.Thrown Throwable failure){if(StorageProviders.packEndPart(failure==null))handled=true;}
 private ProviderPackPartAdvice(){}
}
