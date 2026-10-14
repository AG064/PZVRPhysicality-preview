package pzvrphysicality;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.nio.FloatBuffer;
import java.util.*;
import viewpoint.render.ChunkMeshData;
import viewpoint.world.ChunkCache;
import viewpointvr.physics.CollisionShape;
import zombie.characters.IsoPlayer;
import zombie.iso.*;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.*;
import zombie.iso.sprite.IsoSprite;

/** Passive copies of completed native chunk triangles. No mesher, renderer or solver is invoked. */
public final class WorldContactCapture {
 public static final String DOOR_SHA256="6b13cf9f087d9fd4a1ba845001b9cb4e9462e7c7568fe5213e0e2975a964df7f";
 public static final String BARRICADE_SHA256="cbe10c19938bee69e93da02bb4058da433f7180d41d2f9f375d185e9d177dab0";
 static final int CHUNKS=32,OBJECTS=512,TRIANGLES=24000,MEMORY=8*1024*1024;
 static final double STOREY=Math.sqrt(6);
 private static final Object lock=new Object();
 private static final WeakHashMap<Object,Receipt> recipes=new WeakHashMap<>();
 private static final WeakHashMap<Object,Copied> copies=new WeakHashMap<>();
 private static final WeakHashMap<Object,Long> failed=new WeakHashMap<>();
 private static final WeakHashMap<IsoChunk,Map<Integer,Receipt>> latest=new WeakHashMap<>();
 private static boolean items,storage,held,playerCollision;private static Object actor,cell;private static Thread thread;private static long epoch;
 private static volatile long captured,missing,rejected;private static volatile boolean unavailable;private static boolean barricades;
 private static Field entries,levels,dirty,evicted,version,current,verts,plantBase,retired,recipeLevel,recipeBatches,batchOps,opKind,opOwner,opMesh,tileData;
 public static synchronized void requestItems(boolean value){items=value;}
 public static synchronized void requestStorage(boolean value){storage=value;}
 public static synchronized void requestHeldContacts(boolean value){held=value;}
 public static synchronized void requestPlayerCollision(boolean value){playerCollision=value;}
 public static synchronized boolean enabled(){return items||storage||held||playerCollision;}

 static record Receipt(WeakReference<IsoChunk> chunk,int wx,int wy,int level,WeakReference<Object> actor,WeakReference<Object> cell,long epoch,long version,WeakReference<Object> previous,List<SquareState> squares,boolean unownedStatic){}
 static record Copied(Receipt receipt,Map<Integer,CollisionShape> shapes,CollisionShape unowned,int bytes,int omitted){}
 private record SpriteState(WeakReference<IsoSprite> source,int id,boolean floor,boolean solid,boolean transparent,List<?> flags,List<String> properties){
  static SpriteState read(IsoSprite sprite){
   if(sprite==null)return null;var p=sprite.getProperties();var flags=p.getFlagsList();var names=p.getPropertyNames();
   if(flags.size()>128||names.size()>64)throw new IllegalArgumentException("World sprite metadata budget");
   var rows=new ArrayList<String>();int extent=0;for(String name:names){String value=p.get(name);if(name==null||name.length()>256||value!=null&&value.length()>1024||(extent+=name.length()+(value==null?0:value.length()))>8192)throw new IllegalArgumentException("World sprite metadata");rows.add(name);rows.add(value);}
   return new SpriteState(new WeakReference<>(sprite),sprite.getID(),sprite.solidfloor,sprite.solid,sprite.solidTrans,List.copyOf(flags),Collections.unmodifiableList(rows));
  }
  boolean matches(SpriteState now){return now!=null&&source.get()==now.source.get()&&id==now.id&&floor==now.floor&&solid==now.solid&&transparent==now.transparent&&flags.equals(now.flags)&&properties.equals(now.properties);}
  int bytes(){int n=128+flags.size()*16+properties.size()*24;for(String s:properties)if(s!=null)n+=s.length()*2;return n;}
 }
 private record ObjectState(WeakReference<IsoObject> source,SpriteState sprite,SpriteState overlay,Object direction,float rise,int state,boolean attachment){
  static ObjectState read(IsoObject object){
   if(object==null||object.getObjectIndex()<0)throw new IllegalArgumentException("World object identity");
   float rise=viewpoint.world.WorldMesher.rise(object);if(!Float.isFinite(rise)||Math.abs(rise)>1)throw new IllegalArgumentException("World object rise");
   int value=0;if(object instanceof IsoWindow w)value=(w.IsOpen()?1:0)|(w.isSmashed()?2:0)|(w.isGlassRemoved()?4:0)|(w.isDestroyed()?8:0)|(w.getNorth()?16:0);
   else if(object instanceof IsoDoor d)value=(d.IsOpen()?1:0)|(d.isDestroyed()?8:0);
   else if(object instanceof IsoThumpable t)value=(t.IsOpen()?1:0)|(t.isDestroyed()?8:0)|(t.isDoor()?16:0);
   else if(object instanceof IsoBarricade b)value=(b.isDestroyed()?8:0)|(b.isMetal()?16:0)|(b.isMetalBar()?32:0)|(b.getNumPlanks()<<6);
   return new ObjectState(new WeakReference<>(object),SpriteState.read(object.getSprite()),SpriteState.read(object.getOverlaySprite()),object.getDir(),rise,value,object.attachedAnimSprite!=null&&!object.attachedAnimSprite.isEmpty());
  }
  boolean matches(IsoObject object,int owner){
   if(source.get()!=object)return false;var now=read(object);
   boolean sameState=state==now.state;
   // A north pane's glass state does not alter completed west-wall triangles sharing its square.
   if(object instanceof IsoWindow window&&(owner==1||owner==2)&&owner!=(window.getNorth()?1:2))sameState=(state&16)==(now.state&16);
   return (sprite==null?now.sprite==null:sprite.matches(now.sprite))&&(overlay==null?now.overlay==null:overlay.matches(now.overlay))&&direction==now.direction&&rise==now.rise&&sameState&&attachment==now.attachment;
  }
  int bytes(){return 192+(sprite==null?0:sprite.bytes())+(overlay==null?0:overlay.bytes());}
 }
 static record SquareState(WeakReference<IsoGridSquare> source,int x,int y,int z,List<ObjectState> objects,int mask){
  boolean matches(IsoGridSquare square,int owner){
   if(source.get()!=square||square==null||square.getX()!=x||square.getY()!=y||square.getZ()!=z)return false;
   var list=square.getObjects();if(list.size()!=objects.size())return false;
   for(int i=0;i<list.size();i++)if(list.get(i)==null||list.get(i).getSquare()!=square||!objects.get(i).matches(list.get(i),owner))return false;
   return true;
  }
  int bytes(){return 128+objects.stream().mapToInt(ObjectState::bytes).sum();}
 }
 private static boolean flag(IsoSprite sprite,String name){return sprite!=null&&sprite.getProperties().has(IsoFlagType.valueOf(name));}
 private static int kind(IsoSprite sprite){
  if(sprite==null)return -1;if(sprite.solidfloor)return 3;
  boolean n=false,w=false;for(String name:List.of("doorN","WindowN","windowN","attachedN","collideN","transparentN","WallNTrans","HoppableN","TallHoppableN","DoorWallN","WallN"))n|=flag(sprite,name);
  for(String name:List.of("doorW","WindowW","windowW","attachedW","collideW","transparentW","WallWTrans","HoppableW","TallHoppableW","DoorWallW","WallW"))w|=flag(sprite,name);
  if(n&&w||flag(sprite,"WallNW")||flag(sprite,"attachedNW"))return -1;return n?1:w?2:0;
 }
 static SquareState snapshot(IsoGridSquare square){
  var list=square.getObjects();var objects=new ArrayList<ObjectState>();boolean barrier=false,contentsSafe=true;int mask=0,panes=0,unsupported=0;
  if(list.size()>128)throw new IllegalArgumentException("World square object budget");
  for(var object:list){
   if(object==null||object.getSquare()!=square)throw new IllegalArgumentException("World square ownership");var state=ObjectState.read(object);objects.add(state);int k=kind(object.getSprite());
   if(object instanceof IsoWindow window){panes|=1<<(window.getNorth()?1:2);if(k==0)contentsSafe=false;}
   else if(object instanceof IsoBarricade){barrier=true;if((state.state&8)!=0)contentsSafe=false;}
   else if(k==0)contentsSafe=false;
   if(k==1||k==2)mask|=1<<k;
   if(state.attachment){if(k==1||k==2)unsupported|=1<<k;else contentsSafe=false;}
  }
  mask&=~(panes|unsupported);if(barrier&&contentsSafe&&barricades)mask|=1;
  return new SquareState(new WeakReference<>(square),square.getX(),square.getY(),square.getZ(),List.copyOf(objects),mask);
 }
 private static Field field(Class<?> type,String name,Class<?> expected)throws ReflectiveOperationException{var f=type.getDeclaredField(name);if(f.getType()!=expected)throw new NoSuchFieldException(type.getName()+"."+name);f.setAccessible(true);return f;}
 private static boolean access(long now){
  if(entries!=null)return true;if(unavailable)return false;
  try{
   SdkPins.verify(ChunkCache.class,SdkPins.VIEWPOINT);
   provider("vpdoors.Sheets",DOOR_SHA256);barricades=provider("vpbarricades.Barricades",BARRICADE_SHA256);
   var e=Class.forName("viewpoint.world.ChunkCache$Entry",false,ChunkCache.class.getClassLoader());var l=Class.forName("viewpoint.world.ChunkCache$Level",false,ChunkCache.class.getClassLoader());
   var es=field(ChunkCache.class,"entries",HashMap.class);var ls=e.getDeclaredField("levels");if(!ls.getType().isArray()||ls.getType().componentType()!=l)throw new NoSuchFieldException("Native chunk level layout");ls.setAccessible(true);
   dirty=field(l,"dirty",boolean.class);evicted=field(l,"evicted",boolean.class);version=field(l,"version",long.class);current=field(l,"data",ChunkMeshData.class);
   verts=field(ChunkMeshData.class,"verts",FloatBuffer.class);plantBase=field(ChunkMeshData.class,"plantBase",int.class);retired=field(ChunkMeshData.class,"retired",boolean.class);
   var recipe=Class.forName("viewpoint.world.Recipe",false,ChunkCache.class.getClassLoader());var batch=Class.forName("viewpoint.world.Recipe$Batch",false,ChunkCache.class.getClassLoader());var op=Class.forName("viewpoint.world.Recipe$Op",false,ChunkCache.class.getClassLoader());var tile=Class.forName("viewpoint.world.TileMesh",false,ChunkCache.class.getClassLoader());
   recipeLevel=field(recipe,"level",int.class);recipeBatches=field(recipe,"batches",ArrayList.class);batchOps=field(batch,"ops",ArrayList.class);opKind=field(op,"kind",int.class);opOwner=field(op,"owner",int.class);opMesh=field(op,"mesh",tile);tileData=field(tile,"data",float[].class);levels=ls;entries=es;unavailable=false;return true;
  }catch(Exception|LinkageError invalid){unavailable=true;rejected++;return false;}
 }
 private static boolean provider(String name,String hash)throws Exception{try{SdkPins.verify(Class.forName(name,false,WorldContactCapture.class.getClassLoader()),hash);return true;}catch(ClassNotFoundException absent){return false;}}
 private static boolean unownedStatic(Object recipe)throws ReflectiveOperationException{
  int count=0,unknown=0;var batches=(List<?>)recipeBatches.get(recipe);if(batches.size()>256)return false;
  for(var batch:batches)for(var op:(List<?>)batchOps.get(batch)){
   if(++count>4096)return false;if(opOwner.getInt(op)>=0)continue;unknown++;int kind=opKind.getInt(op);if(kind==6||kind==7)continue;
   if(kind!=0&&kind!=1&&kind!=2)return false;Object mesh=opMesh.get(op);if(mesh==null)return false;var data=(float[])tileData.get(mesh);if(data==null||data.length<24||data.length%24!=0||data.length>8000*24)return false;
   float[] lo={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY},hi={Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
   for(int i=0;i<data.length;i+=8)for(int c=0;c<3;c++){float value=data[i+c];if(!Float.isFinite(value)||Math.abs(value)>16)return false;lo[c]=Math.min(lo[c],value);hi[c]=Math.max(hi[c],value);}
   // Unowned flat cards cannot establish a corner barrier. This only admits the original 3D span; it never fills its bounds.
   if(hi[0]-lo[0]<.0001f||hi[1]-lo[1]<.0001f||hi[2]-lo[2]<.0001f)return false;
  }return unknown>0;
 }
 static Object level(IsoChunk chunk,int z)throws ReflectiveOperationException{var entry=((Map<?,?>)entries.get(null)).get(chunk);if(entry==null||z< -32||z>=32)return null;return ((Object[])levels.get(entry))[z+32];}
 static boolean current(IsoChunk chunk,int z,ChunkMeshData data)throws ReflectiveOperationException{var value=level(chunk,z);return value!=null&&!dirty.getBoolean(value)&&!evicted.getBoolean(value)&&current.get(value)==data&&!retired.getBoolean(data);}
 static boolean ownerThread(){return thread==null||thread==Thread.currentThread();}
 static long epoch(){synchronized(lock){return epoch;}}
 private static IsoPlayer local(){var players=IsoPlayer.players;return players==null||players.length==0?null:players[0];}
 static boolean eligible(IsoPlayer p){return Main.ready()&&p!=null&&p==local()&&!p.isDead()&&Float.isFinite(p.getX())&&Float.isFinite(p.getY())&&Float.isFinite(p.getZ())&&Math.abs(p.getX())<=1_000_000&&Math.abs(p.getY())<=1_000_000&&p.getZ()>= -32&&p.getZ()<32&&p.getCell()!=null&&p.getSquare()!=null&&!zombie.network.GameClient.client&&!zombie.network.GameServer.server;}
 static void scope(IsoPlayer p){
  synchronized(lock){var nextCell=p==null?null:p.getCell();if(thread==null)thread=Thread.currentThread();if(actor!=p||cell!=nextCell){recipes.clear();copies.clear();failed.clear();latest.clear();actor=p;cell=nextCell;epoch++;}}
 }
 /** Native owner thread receipt, including before the first rig publication and while modes are Off. */
 public static void gather(IsoChunk chunk,int z,Object recipe,boolean completed){
  if(!Main.ready()||!completed||chunk==null||recipe==null||!ownerThread())return;
  try{
   var p=local();scope(p);if(!eligible(p))return;long now=System.nanoTime();if(!access(now)||recipeLevel.getInt(recipe)!=z||z< -32||z>=32||Math.abs(z-Math.floor(p.getZ()))>1||Math.abs((long)chunk.wx*8-(long)Math.floor(p.getX()))>16||Math.abs((long)chunk.wy*8-(long)Math.floor(p.getY()))>16)return;
   var rows=new ArrayList<SquareState>();int count=0;
   for(int y=0;y<8;y++)for(int x=0;x<8;x++){
    var square=chunk.getGridSquare(x,y,z);if(square==null){rows.add(null);continue;}
    if(square.getChunk()!=chunk||square.getCell()!=p.getCell()||square.getX()!=chunk.wx*8+x||square.getY()!=chunk.wy*8+y||square.getZ()!=z||(count+=square.getObjects().size())>OBJECTS)throw new IllegalArgumentException("Native chunk receipt extent");rows.add(snapshot(square));
   }
   var value=level(chunk,z);var r=new Receipt(new WeakReference<>(chunk),chunk.wx,chunk.wy,z,new WeakReference<>(p),new WeakReference<>(p.getCell()),epoch,value==null?-1:version.getLong(value),new WeakReference<>(value==null?null:current.get(value)),Collections.unmodifiableList(rows),unownedStatic(recipe));
   int receiptBytes=bytes(r);if(receiptBytes>1024*1024)throw new IllegalArgumentException("Native world receipt memory");
   synchronized(lock){
    if(recipes.size()>=CHUNKS||latest.values().stream().mapToInt(Map::size).sum()>=CHUNKS){recipes.clear();latest.clear();}
    if(metadata()+receiptBytes>4*1024*1024){recipes.clear();latest.clear();copies.clear();failed.clear();}
    recipes.put(recipe,r);latest.computeIfAbsent(chunk,k->new HashMap<>()).put(z,r);
   }
  }catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){rejected++;synchronized(lock){recipes.remove(recipe);var v=latest.get(chunk);if(v!=null)v.remove(z);}}
 }
 /** Numeric-only worker callback. Copies buffers before the original upload releases their storage. */
 public static void cooked(Object recipe,ChunkMeshData data,boolean completed){
  Receipt receipt;synchronized(lock){receipt=recipes.remove(recipe);}if(receipt==null)return;
  if(!completed||data==null){synchronized(lock){var map=latest.get(receipt.chunk.get());if(map!=null&&map.get(receipt.level)==receipt)map.remove(receipt.level);}return;}
  try{copy(data,receipt);}catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){rejected++;synchronized(lock){if(failed.size()>=CHUNKS)failed.clear();failed.put(data,System.nanoTime());}}
 }
 static Copied copy(ChunkMeshData data,Receipt r)throws ReflectiveOperationException{
  if(data.level!=r.level||retired.getBoolean(data))throw new IllegalArgumentException("Native chunk level/retirement");
  var buffer=(FloatBuffer)verts.get(data);if(buffer==null){missing++;synchronized(lock){if(failed.size()>=CHUNKS)failed.clear();failed.put(data,System.nanoTime());}return null;}int base=plantBase.getInt(data);
  int[] masks=new int[64];for(int i=0;i<64;i++){var s=r.squares.get(i);masks[i]=s==null?0:s.mask;}
  var shapes=geometry(buffer,data.runs,base,r.level,masks);var unowned=unownedGeometry(buffer,data.runs,base,r.level,r.unownedStatic);int bytes=0,omitted=0;for(var s:shapes.values())bytes+=(s.vertices().length+s.faces().length)*4;if(unowned!=null)bytes+=(unowned.vertices().length+unowned.faces().length)*4;
  for(int i=0;i<data.runs.length;i+=3){int owner=data.runs[i];if(owner<0?unowned==null:owner>255||(owner&3)!=3&&(masks[owner>>2]&(1<<(owner&3)))==0)omitted++;}
  var result=new Copied(r,shapes,unowned,bytes,omitted);
  synchronized(lock){if(r.epoch!=epoch||r.actor.get()!=actor||r.cell.get()!=cell)return null;int used=copies.values().stream().mapToInt(Copied::bytes).sum();if(copies.size()>=CHUNKS||used+bytes>MEMORY)copies.clear();copies.put(data,result);failed.remove(data);captured++;}return result;
 }
 /** Exact cooked triangle list in render (-X,height,-Y), grouped by native visibility owner. */
 public static Map<Integer,CollisionShape> geometry(FloatBuffer source,int[] runs,int base,int level,int[] masks){
  if(source==null||runs==null||masks==null||masks.length!=64||level< -32||level>=32||base<0||base%3!=0||base>TRIANGLES*3||source.remaining()%14!=0||base>source.remaining()/14||runs.length%3!=0||runs.length>4096*3)throw new IllegalArgumentException("Native cooked geometry extent");
  var input=source.asReadOnlyBuffer();int first=input.position();var rows=new LinkedHashMap<Integer,ArrayList<float[]>>();int end=0,total=0;
  for(int i=0;i<runs.length;i+=3){
   int owner=runs[i],start=runs[i+1],count=runs[i+2];if(start<end||start<0||count<=0||start%3!=0||count%3!=0||count>base-start)throw new IllegalArgumentException("Native cooked owner run");end=start+count;
   if(owner<0||owner>255||(owner&3)==3||(masks[owner>>2]&(1<<(owner&3)))==0)continue;
   var triangles=rows.computeIfAbsent(owner,k->new ArrayList<>());int sx=(owner>>2)%8,sy=(owner>>2)/8;
   if(triangles.size()+count/3>8000||(total+=count/3)>TRIANGLES)throw new IllegalArgumentException("Native owner triangle budget");
   for(int vertex=start;vertex<start+count;vertex+=3){var triangle=new float[9];for(int v=0;v<3;v++){int at=first+(vertex+v)*14;triangle[v*3]=-input.get(at)-sx-.5f;triangle[v*3+1]=-input.get(at+2)-sy-.5f;triangle[v*3+2]=(float)(input.get(at+1)-level*STOREY);for(int c=0;c<3;c++)if(!Float.isFinite(triangle[v*3+c])||Math.abs(triangle[v*3+c])>4)throw new IllegalArgumentException("Native cooked position");}if(area(triangle)>1e-14)triangles.add(triangle);}
  }
  var result=new LinkedHashMap<Integer,CollisionShape>();for(var e:rows.entrySet()){if(e.getValue().isEmpty())continue;float[] points=new float[e.getValue().size()*9];int[] indices=new int[points.length/3];int at=0;for(var t:e.getValue()){System.arraycopy(t,0,points,at,9);at+=9;}for(int j=0;j<indices.length;j++)indices[j]=j;result.put(e.getKey(),new CollisionShape(points,indices,new float[0],true,true));}return Collections.unmodifiableMap(result);
 }
 private static double area(float[] p){double ax=p[3]-p[0],ay=p[4]-p[1],az=p[5]-p[2],bx=p[6]-p[0],by=p[7]-p[1],bz=p[8]-p[2];double x=ay*bz-az*by,y=az*bx-ax*bz,z=ax*by-ay*bx;return x*x+y*y+z*z;}
 /** Unambiguously static unknown-owner runs retain their real triangles under a full chunk-state lease. */
 public static CollisionShape unownedGeometry(FloatBuffer source,int[] runs,int base,int level,boolean supported){
  geometry(source,runs,base,level,new int[64]);if(!supported)return null;var data=source.asReadOnlyBuffer();int first=data.position(),count=0;for(int i=0;i<runs.length;i+=3)if(runs[i]==-1)count+=runs[i+2];if(count==0)return null;if(count>8000*3)throw new IllegalArgumentException("Unowned static triangle budget");
  var triangles=new ArrayList<float[]>();for(int i=0;i<runs.length;i+=3)if(runs[i]==-1)for(int v=runs[i+1];v<runs[i+1]+runs[i+2];v+=3){var triangle=new float[9];for(int n=0;n<3;n++){int at=first+(v+n)*14;triangle[n*3]=-data.get(at)-4;triangle[n*3+1]=-data.get(at+2)-4;triangle[n*3+2]=(float)(data.get(at+1)-level*STOREY);for(int c=0;c<3;c++)if(!Float.isFinite(triangle[n*3+c])||Math.abs(triangle[n*3+c])>16)throw new IllegalArgumentException("Unowned static vertex");}if(area(triangle)>1e-14)triangles.add(triangle);}
  if(triangles.isEmpty())return null;float[] vertices=new float[triangles.size()*9];int[] indices=new int[vertices.length/3];int at=0;for(var triangle:triangles){System.arraycopy(triangle,0,vertices,at,9);at+=9;}for(int n=0;n<indices.length;n++)indices[n]=n;return new CollisionShape(vertices,indices,new float[0],true,true);
 }
 static Copied get(ChunkMeshData data){synchronized(lock){return copies.get(data);}}
 static Copied fallback(IsoChunk chunk,int z,ChunkMeshData data)throws ReflectiveOperationException{
  Receipt r;synchronized(lock){Long last=failed.get(data);long now=System.nanoTime();if(last!=null&&now>=last&&now-last<1_000_000_000L)return null;var map=latest.get(chunk);r=map==null?null:map.get(z);}var nativeLevel=level(chunk,z);
  if(r==null||r.previous.get()==data||nativeLevel==null||r.version<0||r.version==Long.MAX_VALUE||version.getLong(nativeLevel)!=r.version+1||!matches(r))return null;return copy(data,r);
 }
 static boolean matches(Receipt r){var p=local();return p!=null&&r.epoch==epoch&&r.actor.get()==p&&r.cell.get()==p.getCell()&&r.chunk.get()!=null&&r.wx==r.chunk.get().wx&&r.wy==r.chunk.get().wy;}
 private static int bytes(Receipt r){return 192+r.squares.stream().filter(Objects::nonNull).mapToInt(SquareState::bytes).sum();}
 private static int metadata(){var all=Collections.newSetFromMap(new IdentityHashMap<Receipt,Boolean>());all.addAll(recipes.values());for(var map:latest.values())all.addAll(map.values());for(var copy:copies.values())all.add(copy.receipt);return all.stream().mapToInt(WorldContactCapture::bytes).sum();}
 public static void clear(){synchronized(lock){recipes.clear();copies.clear();failed.clear();latest.clear();actor=cell=null;thread=null;epoch++;captured=missing=rejected=0;}}
 public static String status(){synchronized(lock){return "world mesh copies "+captured+", retained "+copies.size()+", bytes "+copies.values().stream().mapToInt(Copied::bytes).sum()+", metadata "+metadata()+", missing CPU "+missing+", rejected "+rejected;}}
 private WorldContactCapture(){}
}
