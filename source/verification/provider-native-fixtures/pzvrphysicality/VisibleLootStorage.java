package pzvrphysicality;
/** Controlled provider boundary for the production NativeStorage consumer matrix. Real provider has its separate suite. */
public final class VisibleLootStorage {
 public static zombie.iso.IsoObject owner;public static boolean enabled,source=true,visible=true;public static ShelfBridge.Binding housing;public static StorageProviders.Pose placement;public static viewpoint.render.SceneData admitted,expected;public static int requests;
 public static void configure(boolean value){enabled=value;if(!value)clear();}public static boolean enabled(){return enabled;}public static String status(){return "Authored native consumer provider";}
 public static boolean potential(zombie.iso.IsoObject value){return value!=null&&value==owner;}public static boolean claimed(zombie.iso.IsoObject value){return potential(value);}
 public static void request(zombie.iso.IsoObject value,boolean active){requests++;if(!active)clear();}
 public static void clear(){admitted=null;}public static void forget(){clear();owner=null;housing=null;placement=null;}
 public static void admit(zombie.iso.IsoObject value,viewpoint.render.SceneData scene){if(value==owner)admitted=scene;}
 public static boolean ready(zombie.iso.IsoObject value){return enabled&&potential(value)&&source&&visible&&admitted!=null&&(expected==null||admitted==expected);}
 public static ShelfBridge.Binding binding(zombie.iso.IsoObject value){return enabled&&potential(value)&&source?housing:null;}
 public static StorageProviders.Pose pose(zombie.iso.IsoObject value){return enabled&&potential(value)&&source?placement:null;}
 public static void begin(zombie.iso.IsoObject value){}public static boolean part(zombie.iso.sprite.IsoSprite value){return false;}public static boolean endPart(boolean success){return false;}public static void end(boolean success){}
 public static void emitted(Object recipe,viewpoint.packs.PackBind binding,boolean turns,float x,float y,float height,int square,boolean success){}
}
