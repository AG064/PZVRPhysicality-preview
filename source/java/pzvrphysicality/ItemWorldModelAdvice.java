package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.inventory.InventoryItem;
@Patch(className="zombie.inventory.InventoryItem",methodName="getWorldStaticItem",strictMatch=true)
public final class ItemWorldModelAdvice {
 @Patch.OnExit public static void exit(@Patch.This InventoryItem item,@Patch.Return(readOnly=false) String model){model=ItemModelFallback.resolve(item,model,true);}
 private ItemWorldModelAdvice(){}
}
