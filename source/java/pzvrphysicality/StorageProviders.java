package pzvrphysicality;

import viewpoint.render.SceneData;import viewpoint.packs.PackBind;
import zombie.iso.IsoObject;import zombie.iso.sprite.IsoSprite;import java.util.ArrayDeque;

/** Optional housing adapters. Native inventory ownership remains in NativeStorage and native actions. */
public final class StorageProviders {
 public record Pose(float x,float y,float height,float cos,float sin,float scale){}
 public interface PackObserver {
  default boolean interested(IsoObject owner){return true;}
  default void begin(IsoObject owner){}
  default void begin(IsoObject owner,float x,float y,float height,int square){begin(owner);}
  default void part(IsoSprite sprite){}
  default void endPart(boolean success){}
  default void emitted(Object recipe,PackBind binding,boolean turns,float x,float y,float height,int square,boolean success){}
  default void end(boolean success){}
 }
 private static volatile PackObserver observer;
 private static final class Watch {final PackObserver observer;boolean failed;Watch(PackObserver value){observer=value;}}
 private static final ThreadLocal<ArrayDeque<Watch>> watches=new ThreadLocal<>();private static final ThreadLocal<Integer> overflow=new ThreadLocal<>();
 public static void observePack(PackObserver value){observer=value;}
 private interface Call {void run(PackObserver value);}
 private static void call(Watch watch,Call call){if(watch==null||watch.observer==null||watch.failed)return;try{call.run(watch.observer);}catch(RuntimeException|LinkageError unavailable){watch.failed=true;}}
 private static Watch watch(){var list=watches.get();return overflow.get()!=null||list==null?null:list.peekLast();}
 public static void packBegin(IsoObject owner){packBegin(owner,Float.NaN,Float.NaN,Float.NaN,-1);}
 public static void packBegin(IsoObject owner,float x,float y,float height,int square){VisibleLootStorage.begin(owner);var list=watches.get();var value=observer;try{if(value!=null&&!value.interested(owner))value=null;}catch(RuntimeException|LinkageError invalid){value=null;}if(value==null&&list==null)return;if(list==null){list=new ArrayDeque<>();watches.set(list);}if(list.size()>=8){for(var watch:list)watch.failed=true;var depth=overflow.get();overflow.set(depth==null?1:depth+1);return;}var watch=new Watch(value);list.addLast(watch);call(watch,v->v.begin(owner,x,y,height,square));}
 public static boolean packPart(IsoSprite sprite){var watch=watch();if(watch!=null&&watch.observer!=null&&!watch.failed)call(watch,v->v.part(sprite));return VisibleLootStorage.part(sprite);}
 public static boolean packEndPart(boolean success){var watch=watch();if(watch!=null&&watch.observer!=null&&!watch.failed)call(watch,v->v.endPart(success));return VisibleLootStorage.endPart(success);}
 public static void packEmitted(Object recipe,PackBind binding,boolean turns,float x,float y,float height,int square,boolean success){VisibleLootStorage.emitted(recipe,binding,turns,x,y,height,square,success);var watch=watch();if(watch!=null&&watch.observer!=null&&!watch.failed)call(watch,v->v.emitted(recipe,binding,turns,x,y,height,square,success));}
 public static void packEnd(boolean success){try{VisibleLootStorage.end(success);}finally{var depth=overflow.get();if(depth!=null){if(depth<=1)overflow.remove();else overflow.set(depth-1);}else{var list=watches.get();if(list!=null){var watch=list.pollLast();if(list.isEmpty())watches.remove();if(watch!=null&&watch.observer!=null)try{watch.observer.end(success&&!watch.failed);}catch(RuntimeException|LinkageError unavailable){/* Passive observers never replace native behavior. */}}}}}
 public static void configure(boolean visibleLoot){VisibleLootStorage.configure(visibleLoot);}
 public static boolean enabled(){return VisibleLootStorage.enabled();}
 public static String status(){return VisibleLootStorage.status();}
 static boolean visible(IsoObject owner){return VisibleLootStorage.claimed(owner)||VisibleLootStorage.potential(owner);}
 /** Necessary selection permission only. An individual housing still requires its current admission receipt. */
 public static boolean selectionAllowed(IsoObject owner){try{return owner!=null&&(SdkPins.shelvesAvailable()||enabled()&&visible(owner));}catch(RuntimeException|LinkageError unavailable){return false;}}
 public static boolean supported(IsoObject owner){return visible(owner)||ShelfBridge.supported(owner);}
 public static void request(IsoObject owner,boolean active){
  if(visible(owner)){ShelfBridge.clear();VisibleLootStorage.request(owner,active);}
  else {VisibleLootStorage.clear();ShelfBridge.request(owner,active);}
 }
 public static ShelfBridge.Binding binding(IsoObject owner){return visible(owner)?VisibleLootStorage.binding(owner):ShelfBridge.binding(owner);}
 public static Pose pose(IsoObject owner){return visible(owner)?VisibleLootStorage.pose(owner):null;}
 public static boolean ready(IsoObject owner){return visible(owner)?VisibleLootStorage.ready(owner):ShelfBridge.ready(owner);}
 public static void admit(IsoObject owner,SceneData scene){VisibleLootStorage.admit(owner,scene);}
 public static void clear(){VisibleLootStorage.forget();ShelfBridge.clear();}
 private StorageProviders(){}
}
