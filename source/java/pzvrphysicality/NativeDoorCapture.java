package pzvrphysicality;

import java.lang.ref.WeakReference;import java.lang.reflect.*;import java.util.*;
import viewpoint.core.Frame;import viewpoint.render.ModelDraws;import viewpointvr.physics.*;
import zombie.characters.IsoPlayer;import zombie.iso.*;import zombie.iso.objects.*;import zombie.core.skinnedmodel.model.ModelMesh;

/** Original object-owned rigid or skinned door draws, copied without advancing animation or rewriting a packet. */
public final class NativeDoorCapture {
 private record Clock(WeakReference<Object> source,float time,boolean finished){
  boolean matches(Clock now){return now!=null&&source.get()==now.source.get()&&time==now.time&&finished==now.finished;}
 }
 private record State(WeakReference<IsoGridSquare> square,WeakReference<Object> sprite,WeakReference<Object> model,WorldContactCapture.SquareState receipt,String name,String animation,String runtime,float time,int nativeState,float rise,Clock clock){
  boolean matches(IsoObject owner)throws ReflectiveOperationException{
   var now=state(owner,false);return now!=null&&square.get()==now.square.get()&&receipt.matches(owner.getSquare(),-1)&&sprite.get()==now.sprite.get()&&model.get()==now.model.get()&&Objects.equals(name,now.name)&&Objects.equals(animation,now.animation)&&Objects.equals(runtime,now.runtime)&&time==now.time&&nativeState==now.nativeState&&rise==now.rise&&(clock==null?now.clock==null:clock.matches(now.clock));
  }
 }
 private static final class Range{
  final Frame frame;final IsoObject owner;final State state;final int first;final long epoch;final List<int[]> excluded=new ArrayList<>();boolean tainted;
  Range(Frame f,IsoObject o,State s,int n,long e){frame=f;owner=o;state=s;first=n;epoch=e;}
 }
 private static final Range OFF=new Range(null,null,null,0,0);
 private static final ThreadLocal<ArrayDeque<Range>> ranges=ThreadLocal.withInitial(ArrayDeque::new);private static final ThreadLocal<Integer> overflow=ThreadLocal.withInitial(()->0);
 private record Part(WeakReference<ModelMesh> mesh,DoorMeshCpu.Data data,ItemMeshCpu.Data rigid){}
 private record Copied(State state,List<Part> parts,CollisionShape shape,float[] pose,long epoch,WeakReference<Object> actor,WeakReference<Object> cell,int bytes){}
 private static final WeakHashMap<IsoObject,Copied> cache=new WeakHashMap<>();private static final List<WeakReference<IsoObject>> owned=new ArrayList<>();
 private static long epoch=-1,captured,missing,stale;private static Field meshes,values,palette,palettes,bones,animationInstance,animatedObjects,animatedOwner,animatedTrack,cachedModel,modelSprite,renderEntries,renderLevels,renderModelled,renderNear,renderEvicted;private static Method clockTime,clockFinished;
 private static boolean renderUnavailable;
 private static void scope(){long value=WorldContactCapture.epoch();if(value!=epoch){cache.clear();owned.clear();epoch=value;}}
 private static IsoPlayer player(){var values=IsoPlayer.players;return values==null||values.length==0?null:values[0];}
 private static Field field(Class<?> type,String name)throws ReflectiveOperationException{var value=type.getDeclaredField(name);value.setAccessible(true);return value;}
 private static Clock clock(IsoObject source)throws ReflectiveOperationException{
  if(animationInstance==null){var type=Class.forName("zombie.core.skinnedmodel.model.IsoObjectAnimations",false,NativeDoorCapture.class.getClassLoader());var entry=Class.forName("zombie.core.skinnedmodel.model.IsoObjectAnimations$AnimatedObject",false,type.getClassLoader());var track=Class.forName("zombie.core.skinnedmodel.animation.AnimationTrack",false,type.getClassLoader());animationInstance=field(type,"instance");animatedObjects=field(type,"animatedObjects");animatedOwner=field(entry,"object");animatedTrack=field(entry,"track");clockTime=track.getMethod("getCurrentTimeValue");clockFinished=track.getMethod("isFinished");if(clockTime.getReturnType()!=float.class||clockFinished.getReturnType()!=boolean.class)throw new NoSuchMethodException("Door animation clock");}
  Object api=animationInstance.get(null);if(api==null)return null;var list=(List<?>)animatedObjects.get(api);if(list.size()>512)throw new IllegalArgumentException("Door animation registry budget");Object found=null;
  for(Object value:list)if(value!=null&&animatedOwner.get(value)==source){if(found!=null)throw new IllegalArgumentException("Ambiguous native door animation");found=animatedTrack.get(value);}
  if(found==null)return null;float time=(Float)clockTime.invoke(found);if(!Float.isFinite(time)||time<0||time>3600)throw new IllegalArgumentException("Door animation time");return new Clock(new WeakReference<>(found),time,(Boolean)clockFinished.invoke(found));
 }
 private static State state(IsoObject owner,boolean receipt)throws ReflectiveOperationException{
  int value;if(owner instanceof IsoDoor d){value=(d.IsOpen()?1:0)|(d.isDestroyed()?2:0);int pair=IsoDoor.getDoubleDoorIndex(owner);if((pair==1||pair==4)&&IsoDoor.getDoubleDoorObject(owner,pair==1?2:3)==null)return null;}else if(owner instanceof IsoThumpable t&&t.isDoor())value=(t.IsOpen()?1:0)|(t.isDestroyed()?2:0);else return null;
  if(cachedModel==null){cachedModel=field(IsoObject.class,"spriteModel");modelSprite=field(IsoObject.class,"spriteModelInit");}
  var square=owner.getSquare();var sprite=owner.getSprite();var model=(zombie.iso.SpriteModel)cachedModel.get(owner);if((value&2)!=0||square==null||sprite==null||model==null||modelSprite.get(owner)!=sprite||owner.getObjectIndex()<0)return null;
  float time=model.getAnimationTime(),rise=viewpoint.world.WorldMesher.rise(owner);if(!Float.isFinite(time)||!Float.isFinite(rise)||Math.abs(rise)>1)throw new IllegalArgumentException("Door native state");
  for(String text:List.of(Objects.toString(model.getModelScriptName(),""),Objects.toString(model.getAnimationName(),""),Objects.toString(model.getRuntimeString(),"")))if(text.length()>256)throw new IllegalArgumentException("Door model metadata budget");
  return new State(new WeakReference<>(square),new WeakReference<>(sprite),new WeakReference<>(model),receipt?WorldContactCapture.snapshot(square):null,model.getModelScriptName(),model.getAnimationName(),model.getRuntimeString(),time,value,rise,clock(owner));
 }
 /** Balanced disabled scopes prevent a nested original drawer from borrowing its caller's owner. */
 public static void begin(Frame frame,IsoObject owner){
  var stack=ranges.get();if(overflow.get()>0||stack.size()>=8){for(var outer:stack)if(outer!=OFF)outer.tainted=true;overflow.set(overflow.get()+1);return;}Range value=OFF;
  if(frame!=null&&stack.stream().anyMatch(outer->outer.owner!=null&&outer.frame==frame))value=new Range(frame,null,null,frame.scene.models.count(),WorldContactCapture.epoch());
  if(Main.ready()&&WorldContactCapture.enabled()&&WorldContactCapture.ownerThread()&&frame!=null&&owner!=null)try{
   var p=player();if(WorldContactCapture.eligible(p)){WorldContactCapture.scope(p);scope();var state=state(owner,true);var square=owner.getSquare();if(state!=null&&square.getCell()==p.getCell()&&Math.abs(square.getX()-p.getX())<=3&&Math.abs(square.getY()-p.getY())<=3&&square.getZ()==(int)Math.floor(p.getZ()))value=new Range(frame,owner,state,frame.scene.models.count(),epoch);}
  }catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){missing++;}stack.push(value);
 }
 public static void end(Frame frame,IsoObject owner,boolean completed){
  if(overflow.get()>0){overflow.set(overflow.get()-1);return;}var stack=ranges.get();if(stack.isEmpty())return;var r=stack.pop();if(r==OFF)return;
  for(var outer:stack)if(outer.owner!=null&&outer.frame==r.frame){if(outer.excluded.size()>=16||r.frame!=frame||r.first<outer.first)outer.tainted=true;else outer.excluded.add(new int[]{r.first,r.frame.scene.models.count()});break;}
  if(r.owner==null)return;
  if(!completed||r.frame!=frame||r.owner!=owner||!Main.ready()||!WorldContactCapture.enabled()||!WorldContactCapture.ownerThread()){if(r.owner==owner&&WorldContactCapture.ownerThread())cache.remove(owner);return;}
  try{
   scope();var p=player();if(r.tainted){cache.remove(owner);return;}if(r.epoch!=epoch||!WorldContactCapture.eligible(p)||!r.state.matches(owner))return;
   if(meshes==null){meshes=field(ModelDraws.class,"meshes");values=field(ModelDraws.class,"values");palette=field(ModelDraws.class,"palette");palettes=field(ModelDraws.class,"palettes");bones=field(ModelDraws.class,"bones");}
   var draws=frame.scene.models;int last=draws.count();var sources=(ModelMesh[])meshes.get(draws);var rows=(float[])values.get(draws);var indices=(int[])palette.get(draws);var poses=(float[])palettes.get(draws);int count=bones.getInt(draws);
   if(r.first<0||last<=r.first||last-r.first>8||last>sources.length||last>indices.length||last>rows.length/68||count<0||count>4096||count>poses.length/12)throw new IllegalArgumentException("Door native packet extent");
   var square=owner.getSquare();var anchor=RigidPhysics.pose(square.getX()+.5f,square.getY()+.5f,(float)(square.getZ()*Math.sqrt(6))+r.state.rise);var parts=new ArrayList<Part>();var shapes=new ArrayList<CollisionShape>();int vertexCount=0,indexCount=0;
   for(int n=r.first;n<last;n++){
    boolean excluded=false;for(var span:r.excluded)if(n>=span[0]&&n<span[1]){excluded=true;break;}if(excluded)continue;
    var mesh=sources[n];if(mesh==null)throw new IllegalArgumentException("Door native mesh unavailable");var data=DoorMeshCpu.get(mesh);var rigid=data==null&&mesh.skinningData==null?ItemMeshCpu.get(mesh):null;
    if(data==null&&rigid==null)throw new IllegalArgumentException("Door original CPU mesh unavailable");
    float[] points;if(data!=null){points=DoorMeshCpu.skin(data,poses,indices[n],count);if(indices[n]>=0&&data.weighted()&&r.state.clock==null)throw new IllegalArgumentException("Door native animation receipt unavailable");}else points=rigid.vertices();
    int[] faces=data==null?rigid.faces():data.faces();if((vertexCount+=points.length/3)>8000||(indexCount+=faces.length)>48000)throw new IllegalArgumentException("Door combined geometry budget");
    shapes.add(ItemMesh.fromRendered(points,faces,Arrays.copyOfRange(rows,n*68,n*68+16),frame.camX,frame.camY,frame.camZ,anchor));parts.add(new Part(new WeakReference<>(mesh),data,rigid));
   }
   if(parts.isEmpty())throw new IllegalArgumentException("No owner-local door records");if(!r.state.matches(owner))return;float[] vertices=new float[vertexCount*3];int[] faces=new int[indexCount];int at=0,index=0;for(var shape:shapes){var points=shape.vertices();System.arraycopy(points,0,vertices,at,points.length);for(int f:shape.faces())faces[index++]=at/3+f;at+=points.length;}
   int bytes=(vertices.length+faces.length)*4+r.state.receipt.bytes()+1024;if(bytes>1024*1024)throw new IllegalArgumentException("Door state memory budget");if(cache.size()>=32&&!cache.containsKey(owner)||cache.values().stream().mapToInt(Copied::bytes).sum()+bytes>8*1024*1024)cache.clear();
   var shape=new CollisionShape(vertices,faces,new float[0],true,true);var previous=cache.get(owner);
   if(previous!=null&&previous.epoch==epoch&&previous.state.matches(owner)&&sameParts(previous.parts,parts)&&CollisionShape.near(previous.pose,anchor)&&previous.shape.sameGeometry(shape))shape=previous.shape;
   cache.put(owner,new Copied(r.state,List.copyOf(parts),shape,anchor,epoch,new WeakReference<>(p),new WeakReference<>(p.getCell()),bytes));captured++;
  }catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){cache.remove(owner);missing++;}
 }
 private static boolean sameParts(List<Part> first,List<Part> second){if(first.size()!=second.size())return false;for(int i=0;i<first.size();i++){var a=first.get(i);var b=second.get(i);if(a.mesh.get()!=b.mesh.get()||a.data!=b.data||a.rigid!=b.rigid)return false;}return true;}
 public static List<PhysicalWorld.Static> sample(IsoPlayer p){
  if(!Main.ready()||!WorldContactCapture.enabled()||!WorldContactCapture.ownerThread()||!WorldContactCapture.eligible(p))return List.of();scope();owned.clear();var result=new ArrayList<PhysicalWorld.Static>();int triangles=0;
  for(var e:cache.entrySet())try{
   var source=e.getKey();var c=e.getValue();var square=source.getSquare();if(c.epoch!=epoch||c.actor.get()!=p||c.cell.get()!=p.getCell()||!c.state.matches(source)||square.getCell()!=p.getCell()||Math.abs(square.getX()-p.getX())>3||Math.abs(square.getY()-p.getY())>3||square.getZ()!=(int)Math.floor(p.getZ())){stale++;continue;}
   boolean current=true;for(var part:c.parts){var mesh=part.mesh.get();if(mesh==null||part.data!=null&&DoorMeshCpu.get(mesh)!=part.data||part.rigid!=null&&ItemMeshCpu.get(mesh)!=part.rigid){current=false;break;}}if(!current){stale++;continue;}
   int faces=c.shape.faces().length/3;if(triangles+faces>24000)continue;result.add(new PhysicalWorld.Static("native-door:"+square.getX()+":"+square.getY()+":"+square.getZ()+":"+source.getObjectIndex(),c.shape,c.pose));triangles+=faces;owned.add(new WeakReference<>(source));
  }catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){stale++;}
  return result;
 }
 /** Original renderer ownership persists while geometry is missing, stale or being reloaded. */
 public static boolean owns(IsoObject source){
  if(!Main.ready()||!WorldContactCapture.enabled()||!WorldContactCapture.ownerThread()||source==null||!WorldContactCapture.eligible(player()))return false;
  try{
   if(!(source instanceof IsoDoor)&&!(source instanceof IsoThumpable t&&t.isDoor()))return false;
   var p=player();var square=source.getSquare();if(square==null||square.getCell()!=p.getCell()||source.getObjectIndex()<0||source.getSprite()==null||square.getChunk()==null)return false;
   var list=square.getObjects();int index=source.getObjectIndex();if(index>=list.size()||list.get(index)!=source)return false;
   if(renderEntries==null){if(renderUnavailable)return false;
    try{SdkPins.verify(viewpoint.world.ChunkCache.class,SdkPins.VIEWPOINT);var e=Class.forName("viewpoint.world.ChunkCache$Entry",false,NativeDoorCapture.class.getClassLoader());var l=Class.forName("viewpoint.world.ChunkCache$Level",false,e.getClassLoader());renderEntries=field(viewpoint.world.ChunkCache.class,"entries");renderLevels=field(e,"levels");renderModelled=field(l,"modelled");renderNear=field(l,"modelsNear");renderEvicted=field(l,"evicted");if(renderEntries.getType()!=HashMap.class||renderModelled.getType()!=ArrayList.class||renderNear.getType()!=boolean.class||renderEvicted.getType()!=boolean.class)throw new NoSuchFieldException("Native door renderer selection");}
    catch(Exception|LinkageError unsupported){renderEntries=null;renderUnavailable=true;return false;}
   }
   Object entry=((Map<?,?>)renderEntries.get(null)).get(square.getChunk());int level=square.getZ()+32;if(entry==null||level<0||level>=64)return false;var levels=(Object[])renderLevels.get(entry);if(levels.length!=64)return false;Object actual=levels[level];
   if(actual==null||renderEvicted.getBoolean(actual)||!renderNear.getBoolean(actual))return false;var selected=(List<?>)renderModelled.get(actual);if(selected==null)return false;boolean current=false;for(int n=0;n<Math.min(512,selected.size());n++)if(selected.get(n)==source){current=true;break;}
   // Unsupported selection density omits a possible packed substitute instead of restoring an invisible closed leaf.
   if(!current&&selected.size()<=512)return false;
   // These caches are inspected, never populated. A selected but unready native model must not regain a packed substitute.
   if(cachedModel==null){cachedModel=field(IsoObject.class,"spriteModel");modelSprite=field(IsoObject.class,"spriteModelInit");}
   var model=(zombie.iso.SpriteModel)cachedModel.get(source);Object initializedFor=modelSprite.get(source);
   if(model!=null&&initializedFor==source.getSprite())for(String value:List.of(Objects.toString(model.getModelScriptName(),""),Objects.toString(model.getAnimationName(),""),Objects.toString(model.getRuntimeString(),"")))if(value.length()>256){missing++;break;}
   return source.getSquare()==square&&source.getObjectIndex()==index&&list.get(index)==source;
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){return false;}
 }
 public static void clear(){cache.clear();owned.clear();ranges.remove();overflow.remove();epoch=-1;captured=missing=stale=0;}
 public static String status(){return "native door contacts "+owned.size()+", retained "+cache.size()+", copies "+captured+", missing "+missing+", stale "+stale+"; "+DoorMeshCpu.status();}
 private NativeDoorCapture(){}
}
