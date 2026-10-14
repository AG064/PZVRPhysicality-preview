package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import zombie.iso.IsoGridSquare;
@Patch(className="viewpoint.world.WorldMesher",methodName="square",strictMatch=true)
public final class WindowMotionSquareAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(0) IsoGridSquare square,@Patch.Argument(1) float x,@Patch.Argument(2) float y,@Patch.Argument(3) float z,@Patch.Local("windowEntered") boolean entered){entered=WindowMotion.begin(square,x,y,z);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Local("windowEntered") boolean entered,@Patch.Thrown Throwable failure){if(entered)WindowMotion.end(failure==null);}
 private WindowMotionSquareAdvice(){}
}
