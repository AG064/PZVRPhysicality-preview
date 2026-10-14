package pzvrphysicality;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import viewpoint.packs.ModelPacks;
import viewpoint.packs.PackBind;
import viewpoint.render.PackModel;
import viewpoint.world.Recipe;
import viewpoint.visibility.Owner;
import viewpointvr.physics.CollisionShape;
import viewpointvr.physics.PackedModelGeometry;
import zombie.core.Core;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Moveable;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoWorldInventoryObject;

/** Bridges declared moveable world sprites through the original selected pack recipe, never native FBX aliases. */
public final class MoveableModels {
 private static final double STOREY=Math.sqrt(6);
 private record Access(Field recipe,Field on,Field bound,Method ready,Field owner,Field modelsMatter,Field models){}
 private record Watch(WeakReference<IsoWorldInventoryObject> source,WeakReference<IsoGridSquare> square,String sprite,PackBind provider,PackedModelGeometry.Mesh mesh,MoveableMesh.Fit fit,float x,float y,float z,float yaw,float scale,int generation,long epoch,long itemEpoch,boolean full){}
 private record Added(InventoryItem item,Watch watch,Object op){}
 private static final class Scope {final IsoGridSquare square;final Recipe recipe;final boolean[] pending;final float x,y,z;final long epoch;final int generation;final ArrayList<Added> added=new ArrayList<>();InventoryItem item;Watch selected;Object op;Scope(IsoGridSquare square,Recipe recipe,boolean[] pending,float x,float y,float z){this.square=square;this.recipe=recipe;this.pending=pending;this.x=x;this.y=y;this.z=z;this.epoch=MoveableModels.epoch;this.generation=ModelPacks.generation();}}
 private static final ThreadLocal<ArrayDeque<Scope>> scopes=new ThreadLocal<>();
 private static final WeakHashMap<InventoryItem,Watch> watched=new WeakHashMap<>();
 private record GroundSource(WeakReference<IsoWorldInventoryObject> source,WeakReference<IsoGridSquare> square,WeakReference<Object> recipe,String sprite,long epoch,boolean full){}
 public record Ground(InventoryItem item,IsoWorldInventoryObject source,IsoGridSquare square,Object recipe,String sprite,long epoch,boolean full){}
 private static final WeakHashMap<InventoryItem,GroundSource> grounds=new WeakHashMap<>();
 private static final Object visibilityConsumer=new Object();
 private static volatile boolean enabled;private static volatile long epoch;private static volatile Access access;
 private static long nextPoll,emitted,fallback,invalid;private static volatile boolean redrawRequested;private static String reason="idle";
 public static boolean enabled(){return enabled;}
 public static boolean packOnly(InventoryItem item){var name=sprite(item);if(name==null||name.isBlank())return false;try{return !ItemModelRenderer.itemHasModel(item);}catch(RuntimeException|LinkageError unavailable){return true;}}
 /** A pack-only moveable cannot enter the original full-quaternion fallback-box simulation. */
 public static boolean canSimulate(InventoryItem item){
  var name=sprite(item);if(name==null||name.isBlank())return true;
  try{if(ItemModelRenderer.itemHasModel(item))return true;return MoveableRenderHook.canSimulate(item);}catch(RuntimeException|LinkageError unsupported){return false;}
 }
 public static long epoch(){return epoch;}
 public static void configure(boolean next){WorldRigCapture.requestMoveables(next);NativeOwnerVisibility.request(visibilityConsumer,next);AuthoredDepth.request(visibilityConsumer,next);if(enabled==next)return;enabled=next;epoch++;MoveableCarry.clear();MoveableDropBootstrap.clear();MoveableRenderHook.reset();List<IsoGridSquare> dirty; synchronized(watched){dirty=watched.values().stream().map(w->w.square.get()).filter(Objects::nonNull).distinct().toList();watched.clear();}synchronized(grounds){for(var g:grounds.values()){var square=g.square.get();if(square!=null&&!dirty.contains(square)){var copy=new ArrayList<>(dirty);copy.add(square);dirty=copy;}}grounds.clear();}for(var square:dirty)invalidate(square);nextPoll=0;}
 public static List<Ground> grounds(){var result=new ArrayList<Ground>();if(!enabled||!Main.ready())return result;synchronized(grounds){for(var entry:grounds.entrySet()){var g=entry.getValue();var item=entry.getKey();var source=g.source.get();var square=g.square.get();if(g.epoch==epoch&&packOnly(item)&&owns(item,source,square)&&Objects.equals(sprite(item),g.sprite))result.add(new Ground(item,source,square,g.recipe.get(),g.sprite,g.epoch,g.full));}}return List.copyOf(result);}
 public static void requestRedraw(){redrawRequested=true;}
 public static void redraw(){synchronized(grounds){for(var g:grounds.values())invalidate(g.square.get());}}
 public static String status(){return "Moveable ground pack models "+(enabled?"On":"Off")+", recipes "+emitted+", native fallback "+fallback+", rejected "+invalid+"; "+reason+"; "+MoveableRenderHook.status();}
 private static Access fields()throws ReflectiveOperationException{
  var a=access;if(a!=null)return a;var loader=MoveableModels.class.getClassLoader();var packs=Class.forName("viewpoint.world.PackGather",false,loader);var world=Class.forName("viewpoint.world.WorldMesher",false,loader);var ready=packs.getDeclaredMethod("ready",PackBind.class);ready.setAccessible(true);
  a=new Access(field(packs,"recipe"),field(packs,"on"),field(packs,"bound"),ready,field(world,"owner"),field(world,"modelsMatter"),field(Recipe.class,"models"));access=a;return a;
 }
 private static Field field(Class<?> type,String name)throws ReflectiveOperationException{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 public static void begin(IsoGridSquare square,float x,float y,float z,boolean[] pending){
  var stack=scopes.get();Scope scope=null;
  if(enabled&&Main.ready()&&square!=null&&pending!=null&&pending.length>0&&Core.getInstance().isOption3DGroundItem())try{var a=fields();var recipe=(Recipe)a.recipe.get(null);if(recipe!=null&&a.on.getBoolean(null))scope=new Scope(square,recipe,pending,x,y,z);}catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){reason="native recipe unavailable";}
  if(scope==null&&stack==null)return;if(stack==null){stack=new ArrayDeque<>();scopes.set(stack);}stack.addLast(scope==null?new Scope(null,null,null,0,0,0):scope);
 }
 private static Scope current(){var stack=scopes.get();return stack==null?null:stack.peekLast();}
 private static boolean alive(Scope s)throws ReflectiveOperationException{return s!=null&&s.recipe!=null&&enabled&&Main.ready()&&s.epoch==epoch&&s.generation==ModelPacks.generation()&&fields().recipe.get(null)==s.recipe&&fields().on.getBoolean(null)&&Core.getInstance().isOption3DGroundItem();}
 private static String sprite(InventoryItem item){return item instanceof Moveable movable?movable.getWorldSprite():null;}
 private static boolean owns(InventoryItem item,IsoWorldInventoryObject source,IsoGridSquare square){return item!=null&&source!=null&&square!=null&&item.getWorldItem()==source&&source.getItem()==item&&item.getContainer()==null&&source.getSquare()==square&&square.getWorldObjects().contains(source)&&item.getScriptItem()!=null&&Boolean.TRUE.equals(item.getScriptItem().isWorldRender());}
 // The original pack shader has yaw only. A saved or simulated quaternion must not be silently flattened.
 private static boolean nativePose(InventoryItem item,IsoWorldInventoryObject source){
  if(item.worldXRotation!=0||item.worldYRotation!=0)return false;var current=NativeItems.visualPose(item,source);var saved=NativeItemPlacement.read(item,source);var pose=current!=null?current:saved==null?null:saved.pose();
  if(pose==null)return true;if(pose.length<7)return false;for(float value:pose)if(!Float.isFinite(value))return false;
  return Math.abs(pose[0]-source.getWorldPosX())<.001&&Math.abs(pose[1]-source.getWorldPosY())<.001&&Math.abs(pose[2]-source.getWorldPosZ()*STOREY)<.001&&Math.abs(pose[3])<.0001&&Math.abs(pose[4])<.0001&&Math.abs(pose[5])<.0001&&Math.abs(Math.abs(pose[6])-1)<.0001;
 }
 private static boolean same(InventoryItem item,Watch w){var source=w.source.get();return enabled&&Main.ready()&&packOnly(item)&&Core.getInstance().isOption3DGroundItem()&&ModelPacks.ring()>0&&w.epoch==epoch&&w.generation==ModelPacks.generation()&&w.itemEpoch==NativeItems.epoch()&&owns(item,source,w.square.get())&&Objects.equals(sprite(item),w.sprite)&&ModelPacks.bind(w.sprite)==w.provider&&w.provider.model.state()==PackModel.RESIDENT&&PackedModelGeometry.get(w.provider.model)==w.mesh&&source.getWorldPosX()==w.x&&source.getWorldPosY()==w.y&&source.getWorldPosZ()==w.z&&item.getWorldZRotation()==w.yaw&&item.worldScale==w.scale&&(w.full?MoveableRenderHook.readyGround(item,source):nativePose(item,source));}
 /** Called after the original native item-model decision. The native decision itself is preserved. */
 public static void select(InventoryItem item,boolean nativeModel){
  var s=current();if(s==null)return;s.item=null;s.selected=null;s.op=null;
  try{if(!alive(s)||nativeModel||item==null)return;var source=item.getWorldItem();var name=sprite(item);if(name==null||name.isBlank()||!owns(item,source,s.square))return;
   synchronized(grounds){if(grounds.size()<256||grounds.containsKey(item))grounds.put(item,new GroundSource(new WeakReference<>(source),new WeakReference<>(s.square),new WeakReference<>(s.recipe),name,epoch,false));}
   NativeOwnerVisibility.watchRecipe(s.recipe,s.square);
   for(var previous:s.added)if(previous.item==item&&same(item,previous.watch)){s.item=item;s.selected=previous.watch;s.op=previous.op;return;}
   if(s.added.size()>=64){fallback++;reason="moveable recipe item budget";return;}
   var binding=ModelPacks.bind(name);if(binding==null){fallback++;reason="no selected binding for "+name;return;}
   var a=fields();a.bound.setBoolean(null,true);boolean ready=Boolean.TRUE.equals(a.ready.invoke(null,binding));
   if(!ready||binding.model.state()!=PackModel.RESIDENT){s.pending[0]=true;fallback++;reason="pack not resident: "+name;return;}
   var mesh=PackedModelGeometry.get(binding.model);if(mesh==null){MeshRecovery.request(binding.model);s.pending[0]=true;fallback++;reason="pack CPU geometry pending: "+name;return;}
   boolean full=MoveableRenderHook.readyGround(item,source);if(!full&&!nativePose(item,source)){fallback++;reason="full item pose retains native fallback";return;}
   var fit=MoveableMesh.fit(binding,mesh,item.getWorldZRotation(),item.worldScale);var w=new Watch(new WeakReference<>(source),new WeakReference<>(s.square),name,binding,mesh,fit,source.getWorldPosX(),source.getWorldPosY(),source.getWorldPosZ(),item.getWorldZRotation(),item.worldScale,s.generation,s.epoch,NativeItems.epoch(),full);
   if(!alive(s)||!same(item,w))return;
   float dx=s.x+source.getX()+source.xoff-s.square.getX()-.5f,dy=s.y+source.getY()+source.yoff-s.square.getY()-.5f,height=s.z+source.zoff*(float)STOREY+.006f;
   if(!Float.isFinite(dx)||!Float.isFinite(dy)||!Float.isFinite(height))throw new IllegalArgumentException("Moveable source pose");
   int owner=a.owner.getInt(null);if(owner<0||owner>=64)throw new IllegalArgumentException("Moveable recipe square");
   if(full){s.op=new Object();}else{Recipe.model(s.recipe,fit.binding(),true,dx,dy,height,owner,0,Owner.of(owner,Owner.CONTENTS));var ops=(List<?>)a.models.get(s.recipe);s.op=ops.get(ops.size()-1);}
   NativeOwnerVisibility.watchRecipe(s.recipe,s.square);s.added.add(new Added(item,w,s.op));s.item=item;s.selected=w;a.modelsMatter.setBoolean(null,true);
   synchronized(grounds){grounds.put(item,new GroundSource(new WeakReference<>(source),new WeakReference<>(s.square),new WeakReference<>(s.recipe),name,epoch,full));}
   synchronized(watched){if(watched.size()>=256&&!watched.containsKey(item)){s.pending[0]=true;rollback(s,s.op);s.item=null;s.selected=null;s.op=null;fallback++;return;}watched.put(item,w);}emitted++;reason="selected pack: "+name;
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){if(s.op!=null)rollback(s,s.op);s.item=null;s.selected=null;s.op=null;invalid++;reason="native bridge rejected: "+unsupported.getClass().getSimpleName();}
 }
 /** Suppress only the current item's exact icon call after its native pack op has been appended. */
 public static boolean skipIcon(float x,float y,float z){var s=current();if(s==null||s.item==null||s.selected==null||s.op==null)return false;try{
   var w=s.selected;var source=w.source.get();boolean matching=alive(s)&&same(s.item,w)&&Math.abs(x-(s.x+source.getX()+source.xoff-s.square.getX()-.5f))<.0001&&Math.abs(y-(s.y+source.getY()+source.yoff-s.square.getY()-.5f))<.0001&&Math.abs(z-(s.z+source.zoff*(float)STOREY+.006f))<.0001;
   if(!matching)rollback(s,s.op);s.item=null;s.selected=null;s.op=null;return matching;
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){rollback(s,s.op);s.item=null;s.selected=null;s.op=null;return false;}}
 private static void rollback(Scope s,Object op){try{var list=(List<?>)fields().models.get(s.recipe);if(list!=null)list.removeIf(v->v==op);}catch(ReflectiveOperationException|RuntimeException ignored){if(s.pending!=null)s.pending[0]=true;}}
 public static void end(boolean completed){var stack=scopes.get();if(stack==null)return;var s=stack.pollLast();if(stack.isEmpty())scopes.remove();if(s==null||s.recipe==null)return;try{boolean valid=completed&&alive(s);for(var added:s.added)if(!valid||!same(added.item,added.watch)){rollback(s,added.op);s.pending[0]=true;}}catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){for(var added:s.added)rollback(s,added.op);s.pending[0]=true;}}
 private static void invalidate(IsoGridSquare square){try{if(square!=null)square.invalidateRenderChunkLevel(256L);}catch(RuntimeException|LinkageError unsupported){reason="native square invalidation unavailable";}}
 /** Native game-thread refresh; no renderer, textures, inventory edits or global chunk invalidation. */
 public static void tick(){if(redrawRequested){redrawRequested=false;redraw();}long now=System.nanoTime();if(now<nextPoll)return;nextPoll=now+100_000_000L;var dirty=new HashSet<IsoGridSquare>();synchronized(watched){var iterator=watched.entrySet().iterator();while(iterator.hasNext()){var entry=iterator.next();if(!same(entry.getKey(),entry.getValue())){var square=entry.getValue().square.get();if(square!=null)dirty.add(square);var source=entry.getValue().source.get();if(source!=null&&source.getSquare()!=null)dirty.add(source.getSquare());iterator.remove();}}}for(var square:dirty)invalidate(square);}
 /** Read-only exact resting geometry. The yaw-only renderer is not a full-quaternion physics provider. */
 public static CollisionShape shape(InventoryItem item,IsoWorldInventoryObject source,long itemEpoch){if(itemEpoch!=NativeItems.epoch())return null;synchronized(watched){var w=watched.get(item);return w!=null&&w.source.get()==source&&same(item,w)?w.fit.shape():null;}}
 private MoveableModels(){}
}
