package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="viewpoint.render.WorldRenderer",methodName="finish",strictMatch=true)
public final class AuthoredDepthScopeAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(0) Object scene){AuthoredDepth.begin(scene);}
 private AuthoredDepthScopeAdvice(){}
}
