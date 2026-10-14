package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.inventory.InventoryItem;
@Patch(className="zombie.inventory.InventoryItem",methodName="getStaticModel",strictMatch=true)
public final class ItemStaticModelAdvice {
 @Patch.OnExit public static void exit(@Patch.This InventoryItem item,@Patch.Return(readOnly=false) String model){model=ItemModelFallback.resolve(item,model,false);}
 private ItemStaticModelAdvice(){}
}
