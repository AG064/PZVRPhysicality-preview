package pzvrphysicality;

import se.krka.kahlua.vm.KahluaTable;
import zombie.inventory.InventoryItem;
import zombie.iso.objects.IsoWorldInventoryObject;

/** Game-thread-only save adapter. Render threads receive detached numeric snapshots through NativeItems. */
public final class NativeItemPlacement {
 private static final String KEY="PZVRDropPoseV1";
 private static boolean own(InventoryItem item,IsoWorldInventoryObject source){
  return item!=null&&source!=null&&source.getItem()==item&&item.getWorldItem()==source&&item.getContainer()==null&&source.getSquare()!=null;
 }
 private static double number(KahluaTable table,String key){Object value=table.rawget(key);if(!(value instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IllegalArgumentException("Saved item field");return n.doubleValue();}
 private static int integer(KahluaTable table,String key){double v=number(table,key);if(v<Integer.MIN_VALUE||v>Integer.MAX_VALUE||v!=Math.rint(v))throw new IllegalArgumentException("Saved item identity");return (int)v;}
 public static SavedDropPose read(InventoryItem item,IsoWorldInventoryObject source){
  if(!own(item,source)||!item.hasModData())return null;
  Object value=item.getModData().rawget(KEY);if(!(value instanceof KahluaTable table))return null;
  try{
   if(number(table,"v")!=1)return null;
   var pose=new SavedDropPose(integer(table,"id"),integer(table,"x"),integer(table,"y"),integer(table,"z"),
    (float)number(table,"ox"),(float)number(table,"oy"),(float)number(table,"oz"),(float)number(table,"dh"),
    (float)number(table,"qx"),(float)number(table,"qy"),(float)number(table,"qz"),(float)number(table,"qw"));
   var square=source.getSquare();return pose.matches(item.getID(),square.getX(),square.getY(),square.getZ(),source.getOffX(),source.getOffY(),source.getOffZ())?pose:null;
  }catch(IllegalArgumentException invalid){return null;}
 }
 public static boolean save(InventoryItem item,IsoWorldInventoryObject source,float[] state){
  if(!own(item,source)||zombie.network.GameClient.client||zombie.network.GameServer.server||state==null||state.length<7)return false;
  for(int n=0;n<7;n++)if(!Float.isFinite(state[n]))return false;
  var square=source.getSquare();
  if(Math.abs(state[0]-source.getWorldPosX())>.01||Math.abs(state[1]-source.getWorldPosY())>.01)return false;
  try{
   var pose=new SavedDropPose(item.getID(),square.getX(),square.getY(),square.getZ(),source.getOffX(),source.getOffY(),source.getOffZ(),
    state[2]-(float)(source.getWorldPosZ()*Math.sqrt(6)),state[3],state[4],state[5],state[6]);
   if(pose.equals(read(item,source)))return true;
   var data=zombie.Lua.LuaManager.platform.newTable();
   String[] fields={"v","id","x","y","z","ox","oy","oz","dh","qx","qy","qz","qw"};
   double[] values={1,pose.id(),pose.x(),pose.y(),pose.z(),pose.ox(),pose.oy(),pose.oz(),pose.dh(),pose.qx(),pose.qy(),pose.qz(),pose.qw()};
   for(int n=0;n<fields.length;n++)data.rawset(fields[n],values[n]);
   item.getModData().rawset(KEY,data);return true;
  }catch(IllegalArgumentException invalid){return false;}
 }
 public static void clear(InventoryItem item){if(item!=null&&item.hasModData())item.getModData().rawset(KEY,null);}
 private NativeItemPlacement(){}
}
