package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.characters.IsoGameCharacter;import zombie.core.skinnedmodel.model.ModelSlotRenderData;
@Patch(className="local.pzvr.NativeRig",methodName="capture",strictMatch=true)
public final class HeldContactScopeAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(0) Object frame,@Patch.Argument(1) ModelSlotRenderData slot,@Patch.Argument(2) IsoGameCharacter actor){HeldContactLimiter.begin(frame,slot,actor);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Thrown Throwable failure){HeldContactLimiter.end(failure);}
 private HeldContactScopeAdvice(){}
}
