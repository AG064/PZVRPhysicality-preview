package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.iso.IsoObject;
@Patch(className="viewpoint.world.PackGather",methodName="object",strictMatch=true)
public final class CounterTapObjectAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(0) IsoObject owner){CounterTapBindings.begin(owner);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(0) IsoObject owner,@Patch.Return boolean handled,@Patch.Thrown Throwable failure){CounterTapBindings.end(owner,handled&&failure==null);}
 private CounterTapObjectAdvice(){}
}
