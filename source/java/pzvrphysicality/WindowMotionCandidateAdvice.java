package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.iso.*;import zombie.iso.sprite.IsoSprite;
@Patch(className="viewpoint.world.WorldMesher",methodName="drawnElsewhere",strictMatch=true)
public final class WindowMotionCandidateAdvice {
 @Patch.OnEnter public static void enter(){WindowMotion.clearCandidate();}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(0) IsoGridSquare square,@Patch.Argument(1) IsoObject object,@Patch.Argument(2) IsoSprite sprite,@Patch.Return boolean skipped,@Patch.Thrown Throwable failure){WindowMotion.candidate(square,object,sprite,skipped||failure!=null);}
 private WindowMotionCandidateAdvice(){}
}
