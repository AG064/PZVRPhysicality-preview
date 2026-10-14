package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.inventory.InventoryItem;
@Patch(className="viewpoint.world.WorldMesher",methodName="hasModel",strictMatch=true)
public final class MoveableModelAdvice {
 @Patch.OnExit public static void exit(@Patch.Argument(0) Object candidate,@Patch.Return boolean original){if(candidate instanceof InventoryItem item)MoveableModels.select(item,original);}
 private MoveableModelAdvice(){}
}
