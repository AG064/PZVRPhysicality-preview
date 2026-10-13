package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="local.pzvr.Bindings",methodName="resolve",strictMatch=true)
public final class GripAdvice {
 @Patch.OnExit public static void exit(@Patch.Argument(1) String key,@Patch.Return(readOnly=false) String value){
  if(GripRoute.gripKey(key)){int side=GripRoute.side(key,local.pzvr.VrApi.dominantHandIndex());value=GripRoute.resolve(NativeItems.ownsGrip(side)||RouteModes.body()&&NativeBody.ownsGrip(side)||RouteModes.curtains()&&NativeCurtains.ownsGrip(side)||RouteModes.backpack()&&BackpackContents.ownsGrip(side),key,value);}
  else if("dominant.trigger".equals(key)||"other.trigger".equals(key)){value=NativeTaps.routeTrigger(key,NativeConsumption.routeTrigger(key,value));if(RouteModes.crafting())value=NativeCrafting.routeTrigger(key,value);if(RouteModes.backpack())value=BackpackContents.routeTrigger(key,value);}
 }
 private GripAdvice(){}
}
