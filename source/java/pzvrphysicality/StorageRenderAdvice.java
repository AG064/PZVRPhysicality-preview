package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.core.Frame;
@Patch(className="viewpoint.models.Models",methodName="snapshot",strictMatch=true)
public final class StorageRenderAdvice {
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(0) Frame frame,@Patch.Thrown Throwable failure){if(failure==null){BodyAnchorCapture.capture(frame);UtensilCapture.capture(frame,zombie.characters.IsoPlayer.players.length==0?null:zombie.characters.IsoPlayer.players[0]);NativeStorage.capture(frame);if(RouteModes.backpack())BackpackCapture.capture(frame);}}
 private StorageRenderAdvice(){}
}
