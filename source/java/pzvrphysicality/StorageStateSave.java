package pzvrphysicality;
import se.krka.kahlua.vm.KahluaTable;
import zombie.inventory.*;

/** Single-player pose commits only. It never performs an inventory transfer or modifies quantity/stat fields. */
public final class StorageStateSave {
 private static final String ITEM="PZVRPlacementV1",OBJECT="PZVRStorageV1";
 private static double number(KahluaTable t,String key){Object value=t.rawget(key);if(!(value instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IllegalArgumentException("Storage saved number");return n.doubleValue();}
 private static int integer(KahluaTable t,String key){double n=number(t,key);if(n!=Math.rint(n)||n<Integer.MIN_VALUE||n>Integer.MAX_VALUE)throw new IllegalArgumentException("Storage saved identity");return (int)n;}
 public static long commit(InventoryItem item,ItemContainer container,String key,long expected,float[] pose){
  if(zombie.network.GameClient.client||zombie.network.GameServer.server||item==null||container==null||key==null||expected<1||item.getContainer()!=container||item.getWorldItem()!=null||!container.contains(item)||pose==null||pose.length!=7)return 0;
  for(float n:pose)if(!Float.isFinite(n))return 0;for(int n=0;n<3;n++)if(pose[n]<-.05||pose[n]>1.05)return 0;
  double norm=pose[3]*pose[3]+pose[4]*pose[4]+pose[5]*pose[5]+pose[6]*pose[6];if(Math.abs(norm-1)>.002)return 0;
  var parent=container.getParent();if(parent==null||container.getContainingItem()!=null)return 0;
  Object owner=parent.getModData().rawget(OBJECT),value=item.getModData().rawget(ITEM);
  if(!(owner instanceof KahluaTable all)||!(all.rawget(Integer.toString(parent.getContainerIndex(container))) instanceof KahluaTable storage)||!(value instanceof KahluaTable old))return 0;
  try{
   int revision=integer(old,"rev"),slot=integer(old,"slot"),global=integer(storage,"rev");
   if(number(storage,"v")!=1||number(old,"v")!=1||!key.equals(storage.rawget("key"))||!key.equals(old.rawget("key"))||integer(old,"id")!=item.getID()||revision!=expected||revision>=2147483646||global<0||global>=2147483646||slot<0||slot>=2048)return 0;
   double dot=number(old,"qx")*pose[3]+number(old,"qy")*pose[4]+number(old,"qz")*pose[5]+number(old,"qw")*pose[6];
   if(Boolean.TRUE.equals(old.rawget("raw"))&&Boolean.FALSE.equals(old.rawget("auto"))&&Math.abs(number(old,"x")-pose[0])<.00001&&Math.abs(number(old,"y")-pose[1])<.00001&&Math.abs(number(old,"z")-pose[2])<.00001&&Math.abs(Math.abs(dot)-1)<.000001)return revision;
   var next=zombie.Lua.LuaManager.platform.newTable();String[] names={"v","id","slot","rev","x","y","z","qx","qy","qz","qw"};double[] fields={1,item.getID(),slot,revision+1,pose[0],pose[1],pose[2],pose[3],pose[4],pose[5],pose[6]};
   for(int n=0;n<names.length;n++)next.rawset(names[n],fields[n]);next.rawset("key",key);next.rawset("auto",false);next.rawset("raw",true);
   item.getModData().rawset(ITEM,next);storage.rawset("rev",(double)(global+1));return revision+1L;
  }catch(IllegalArgumentException invalid){return 0;}
 }
 private StorageStateSave(){}
}
