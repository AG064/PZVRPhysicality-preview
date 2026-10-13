package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;import viewpoint.packs.PackBind;
@Patch(className="pz.viewpointcontainers.BindingSelection",methodName="selected",strictMatch=true)
public final class ShelfSelectionAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(value=0,readOnly=false) int state){if(ShelfBridge.emptyRequested())state=0;}
 @Patch.OnExit public static void exit(@Patch.Argument(0) int state,@Patch.Return PackBind binding){ShelfBridge.selected(binding,state);}
 private ShelfSelectionAdvice(){}
}
