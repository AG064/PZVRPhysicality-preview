package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import local.pzvr.ControllerProfiles;import org.lwjgl.system.MemoryStack;
@Patch(className="local.pzvr.OpenXrBackend",methodName="suggest",strictMatch=true)
public final class ControllerTouchSuggestAdvice {
 @Patch.OnEnter(skipOn=true) public static boolean enter(@Patch.This Object backend,@Patch.Argument(0) ControllerProfiles.Profile profile,@Patch.Argument(1) MemoryStack stack){return ControllerTouch.suggest(backend,profile,stack);}
 private ControllerTouchSuggestAdvice(){}
}
