package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.core.Frame;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoObject;

@Patch(className="viewpoint.models.Models",methodName="own",strictMatch=true)
public final class GroundItemAdvice {
 @Patch.OnEnter public static void enter(@Patch.Argument(0) Frame frame,@Patch.Argument(3) IsoObject object,@Patch.Argument(4) InventoryItem item){GroundItemCapture.begin(frame,item);NativeDoorCapture.begin(frame,object);}
 @Patch.OnExit(onThrowable=Throwable.class) public static void exit(@Patch.Argument(0) Frame frame,@Patch.Argument(3) IsoObject object,@Patch.Argument(4) InventoryItem item,@Patch.Thrown Throwable failure){try{GroundItemCapture.end(frame,item,failure==null);}finally{NativeDoorCapture.end(frame,object,failure==null);}}
 private GroundItemAdvice(){}
}
