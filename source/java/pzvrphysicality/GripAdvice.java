package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="local.pzvr.Bindings",methodName="resolve",strictMatch=true)
public final class GripAdvice {
 @Patch.OnExit public static void exit(@Patch.Argument(1) String key,@Patch.Return(readOnly=false) String value){
  String nativeValue=value;
  try(var scope=NativeInputOwnership.open()){
   int bindingSide=NativeInputOwnership.bindingHandIndex();
   if(GripRoute.gripKey(key)){int side=GripRoute.side(key,bindingSide);if(!scope.snapshot().allows(side))return;value=GripRoute.resolve(NativeItems.ownsGrip(side)||RouteModes.body()&&NativeBody.ownsGrip(side)||RouteModes.curtains()&&NativeCurtains.ownsGrip(side)||RouteModes.backpack()&&BackpackContents.ownsGrip(side),key,value);}
   else if("dominant.trigger".equals(key)||"other.trigger".equals(key)){int side=bindingSide<0?-1:("dominant.trigger".equals(key)?bindingSide:1-bindingSide);if(!scope.snapshot().allows(side))return;value=NativeTaps.routeTrigger(key,NativeConsumption.routeTrigger(key,value));if(RouteModes.crafting())value=NativeCrafting.routeTrigger(key,value);if(RouteModes.backpack())value=BackpackContents.routeTrigger(key,value);}
  }catch(RuntimeException|LinkageError unavailable){value=nativeValue;}
 }
 private GripAdvice(){}
}
