package pzvrphysicality;
import java.lang.ref.WeakReference;import java.lang.reflect.Method;import java.util.*;
import viewpoint.packs.PackBind;import zombie.iso.IsoObject;

/** Scoped stock-state requests and observation of the binding actually submitted by the shelf builder. */
public final class ShelfBridge {
 private record Wanted(WeakReference<IsoObject> owner,long generation,long time){}
 public record Binding(PackBind binding,boolean turns,boolean empty,long generation,long time){}
 private static final class Scope {final IsoObject owner;final long generation;final boolean empty;PackBind selected,emitted;boolean selectedEmpty,turns;Scope(IsoObject owner,long generation,boolean empty){this.owner=owner;this.generation=generation;this.empty=empty;}}
 private static volatile Wanted wanted;private static long generation;private static Method eligible;
 private static final Map<IsoObject,Binding> bindings=new WeakHashMap<>();
 private static final ThreadLocal<ArrayDeque<Scope>> stack=new ThreadLocal<>();
 private static boolean fresh(Wanted value,long now){return value!=null&&value.owner.get()!=null&&now>=value.time&&now-value.time<2_500_000_000L;}
 public static boolean supported(IsoObject owner){
  if(owner==null||owner.getSprite()==null||!Main.ready())return false;
  try{if(eligible==null){var type=Class.forName("pz.viewpointcontainers.ContainerStates",false,ShelfBridge.class.getClassLoader());eligible=type.getDeclaredMethod("eligible",IsoObject.class,zombie.iso.sprite.IsoSprite.class);eligible.setAccessible(true);}return Boolean.TRUE.equals(eligible.invoke(null,owner,owner.getSprite()));}
  catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){return false;}
 }
 /** Called by the game-thread model producer after original native item assets were captured successfully. */
 public static void request(IsoObject owner,boolean enabled){
  IsoObject before;boolean invalidate=false;long now=System.nanoTime();
  synchronized(ShelfBridge.class){before=wanted==null?null:wanted.owner.get();
   if(!enabled||owner==null){if(before!=null){generation++;invalidate=true;}wanted=null;bindings.clear();}
   else {if(before!=owner||!fresh(wanted,now)){generation++;bindings.clear();invalidate=true;}wanted=new Wanted(new WeakReference<>(owner),generation,now);}
  }
  if(invalidate){invalidate(before);if(owner!=before)invalidate(owner);}
 }
 private static void invalidate(IsoObject owner){try{if(owner!=null&&owner.getSquare()!=null)owner.getSquare().invalidateRenderChunkLevel(256L);}catch(RuntimeException|LinkageError failure){System.err.println("[PZVR Storage] Shelf stock invalidation unavailable: "+failure);}}
 private static void expire(){
  IsoObject owner;synchronized(ShelfBridge.class){var value=wanted;if(value==null||fresh(value,System.nanoTime()))return;owner=value.owner.get();generation++;wanted=null;bindings.clear();}invalidate(owner);
 }
 private static void retire(Scope scope){
  synchronized(ShelfBridge.class){var value=wanted;if(value==null||value.generation!=scope.generation||value.owner.get()!=scope.owner)return;generation++;wanted=null;bindings.clear();}invalidate(scope.owner);
 }
 public static void begin(IsoObject owner){var value=wanted;long now=System.nanoTime();if(value!=null&&!fresh(value,now)){expire();value=wanted;}boolean active=Main.ready()&&fresh(value,now)&&value.owner.get()==owner;var values=stack.get();if(!active&&values==null)return;if(values==null){values=new ArrayDeque<>();stack.set(values);}values.addLast(new Scope(owner,active?value.generation:-1,active));}
 private static Scope scope(){var values=stack.get();return values==null?null:values.peekLast();}
 public static boolean emptyRequested(){var value=wanted;if(value==null)return false;if(!fresh(value,System.nanoTime())){expire();value=wanted;}var scope=scope();return scope!=null&&scope.empty&&fresh(value,System.nanoTime())&&scope.generation==value.generation&&value.owner.get()==scope.owner;}
 public static void selected(PackBind binding,int state){var scope=scope();if(scope!=null){scope.selected=binding;scope.selectedEmpty=scope.empty&&state==0;}}
 public static void emitted(PackBind binding,boolean turns){var scope=scope();if(scope!=null&&scope.empty){scope.emitted=binding;scope.turns=turns;}}
 public static void end(boolean success){
  var values=stack.get();if(values==null)return;var scope=values.pollLast();if(values.isEmpty())stack.remove();if(scope==null||!scope.empty)return;if(!success||scope.emitted==null){retire(scope);return;}
  long now=System.nanoTime();synchronized(ShelfBridge.class){var value=wanted;if(!fresh(value,now)||value.generation!=scope.generation||value.owner.get()!=scope.owner)return;
   var previous=bindings.get(scope.owner);boolean empty=scope.selectedEmpty&&scope.emitted==scope.selected||previous!=null&&previous.empty&&previous.binding==scope.emitted&&previous.generation==scope.generation;
   if(bindings.size()>=64&&!bindings.containsKey(scope.owner))bindings.clear();bindings.put(scope.owner,new Binding(scope.emitted,scope.turns,empty,scope.generation,now));
  }
 }
 public static Binding binding(IsoObject owner){var value=wanted;if(value!=null&&!fresh(value,System.nanoTime()))expire();synchronized(ShelfBridge.class){value=wanted;long now=System.nanoTime();if(!fresh(value,now)||value.owner.get()!=owner)return null;var b=bindings.get(owner);return b!=null&&b.generation==value.generation?b:null;}}
 public static boolean ready(IsoObject owner){var b=binding(owner);return b!=null&&b.empty;}
 public static void clear(){request(null,false);}
 private ShelfBridge(){}
}
