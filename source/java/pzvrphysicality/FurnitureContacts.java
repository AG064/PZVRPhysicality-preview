package pzvrphysicality;

import java.util.*;
import viewpoint.packs.ModelPacks;
import viewpoint.packs.PackBind;
import viewpoint.render.PackModel;
import viewpoint.world.ChunkCache;
import viewpoint.world.WorldMesher;
import viewpointvr.physics.PackedModelGeometry;
import viewpointvr.physics.RigidPhysics;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoObject;

/** Bounded, game-thread discovery around loose items. Does not depend on looking at the furniture. */
public final class FurnitureContacts {
 /** Weak native identities plus the existing immutable numerical row from the last owned sample. */
 public static final class CapturedContact {
  private final java.lang.ref.WeakReference<IsoObject> source;private final java.lang.ref.WeakReference<zombie.iso.IsoGridSquare> square;private final java.lang.ref.WeakReference<zombie.iso.sprite.IsoSprite> sprite;private final java.lang.ref.WeakReference<IsoPlayer> actor;private final java.lang.ref.WeakReference<zombie.iso.IsoCell> cell;
  private final int objectIndex;private final String name;private final PackBind binding;private final PackModel model;private final PackedModelGeometry.Mesh mesh;private final PhysicalWorld.Static row;private final TapProfile sinkProfile;private final float cos,sin,scale,x,y,z,rise;private final long capturedAt,generation,inputTime;
  private CapturedContact(IsoObject object,IsoPlayer player,PackBind bind,PackedModelGeometry.Mesh data,PhysicalWorld.Static value,TapProfile profile,float c,float s,WorldRigFrame rig,long now){source=new java.lang.ref.WeakReference<>(object);square=new java.lang.ref.WeakReference<>(object.getSquare());sprite=new java.lang.ref.WeakReference<>(object.getSprite());actor=new java.lang.ref.WeakReference<>(player);cell=new java.lang.ref.WeakReference<>(player.getCell());objectIndex=object.getObjectIndex();name=object.getSprite().name;binding=bind;model=bind.model;mesh=data;row=value;sinkProfile=profile;cos=c;sin=s;scale=bind.scale;x=bind.x;y=bind.y;z=bind.z;rise=WorldMesher.rise(object);capturedAt=now;generation=rig.generation();inputTime=rig.inputTime();}
  public IsoObject source(){return source.get();}public zombie.iso.IsoGridSquare square(){return square.get();}public zombie.iso.sprite.IsoSprite sprite(){return sprite.get();}public IsoPlayer actor(){return actor.get();}public int objectIndex(){return objectIndex;}public PackBind binding(){return binding;}public PackModel model(){return model;}public PackedModelGeometry.Mesh mesh(){return mesh;}public PhysicalWorld.Static row(){return row;}public TapProfile sinkProfile(){return sinkProfile;}public long capturedAt(){return capturedAt;}public long generation(){return generation;}public long inputTime(){return inputTime;}
 }
 private record Fitted(PackModel model,PackedModelGeometry.Mesh mesh,float cos,float sin,float scale,viewpointvr.physics.CollisionShape shape){}
 private final Map<IsoObject,Fitted> cache=new WeakHashMap<>();
 private final Map<IsoObject,CapturedContact> sampledSinks=new WeakHashMap<>();private final NearbyTaps sinkProfiles=new NearbyTaps();private volatile boolean sampleReady;private Thread sampleThread;
 private final Set<IsoObject> requestedCounters=Collections.newSetFromMap(new WeakHashMap<>());
 private NativeWindowContacts windows;private boolean windowChecked;
 private NativeWorldContacts generated;
 private NativeWindowContacts windows(){
  if(!windowChecked){windowChecked=true;try{
   Class.forName("techjungle.betterwindow.WindowAssets",false,FurnitureContacts.class.getClassLoader());
   windows=new NativeWindowContacts();
  }catch(ClassNotFoundException|LinkageError unavailable){windows=null;}}
  return windows;
 }
 private int exact,missing,skipped;private long warning;
 public List<PhysicalWorld.Static> sample(IsoPlayer player) {
  sampleReady=false;sampleThread=Thread.currentThread();sampledSinks.clear();var sampledRig=WorldRigCapture.latest;long sampledAt=System.nanoTime();boolean capture=sampledRig!=null&&sampledRig.actor()==player&&sampledRig.fresh(sampledAt);
  CounterTapBindings.contacts(this,true);
  var windowContacts=windows();var result=new ArrayList<PhysicalWorld.Static>(windowContacts==null?List.of():windowContacts.sample(player));exact=missing=skipped=0;
  if(RouteModes.world()){if(generated==null)generated=new NativeWorldContacts();for(var row:generated.sample(player)){if(result.size()>=64)break;result.add(row);}}
  int x=(int)Math.floor(player.getX()),y=(int)Math.floor(player.getY()),z=(int)Math.floor(player.getZ()),scanned=0,triangles=0;
  outer:for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++){
   var square=player.getCell().getGridSquare(x+dx,y+dy,z);if(square==null||!ChunkCache.packsOn(square))continue;
   var objects=square.getObjects();
   for(int index=0;index<objects.size();index++){
    if(++scanned>512||result.size()>=128)break outer;
    var object=objects.get(index);if(object==null||object.getSquare()!=square||object.getSprite()==null||windowContacts!=null&&windowContacts.owns(object)||RouteModes.world()&&generated!=null&&generated.owns(object))continue;
    try{
     var observed=ShelfBridge.binding(object);var counter=CounterTapBindings.current(object);if(counter==null&&(CounterTapBindings.owns(object)||CounterTapBindings.potential(object))){if(!requestedCounters.contains(object)){if(requestedCounters.size()>=128)requestedCounters.clear();requestedCounters.add(object);object.getSquare().invalidateRenderChunkLevel(256L);}missing++;continue;}var binding=counter!=null?counter.binding():observed==null?ModelPacks.bind(object.getSprite()):observed.binding();if(binding==null)continue;
     int phase=binding.model.state();if(phase!=PackModel.READ&&phase!=PackModel.RESIDENT){missing++;continue;}
     var mesh=PackedModelGeometry.get(binding.model);
     if(mesh==null){MeshRecovery.request(binding.model);missing++;continue;}
     int count=mesh.faces().length/3;if(count>20000||triangles+count>60000){skipped++;continue;}
     boolean turns=counter!=null?counter.turns():observed==null?PackBind.turns(object.getSprite()):observed.turns();float c=turns?binding.cos:1,s=turns?binding.sin:0;
     var old=cache.get(object);
     var shape=old!=null&&old.model==binding.model&&old.mesh==mesh&&old.cos==c&&old.sin==s&&old.scale==binding.scale
       ?old.shape:MeshPlacement.place(mesh,c,s,binding.scale);
     if(cache.size()>=128&&!cache.containsKey(object))cache.clear();
     cache.put(object,new Fitted(binding.model,mesh,c,s,binding.scale,shape));
     var pose=RigidPhysics.pose(square.getX()+.5f+binding.x,square.getY()+.5f+binding.y,
       (float)(square.getZ()*Math.sqrt(6))+WorldMesher.rise(object)+binding.z);
     var row=new PhysicalWorld.Static("packed:"+square.getX()+":"+square.getY()+":"+z+":"+index,shape,pose);result.add(row);exact++;triangles+=count;
     var known=binding.model.obj==null?null:TapProfile.find(binding.model.obj.getName());if(capture&&observed==null&&counter==null&&known!=null&&!known.authored()){var profile=sinkProfiles.checkedProfile(binding.model);if(profile!=null)sampledSinks.put(object,new CapturedContact(object,player,binding,mesh,row,profile,c,s,sampledRig,sampledAt));}
    }catch(RuntimeException unsupported){skipped++;long now=System.nanoTime();if(now-warning>5_000_000_000L){warning=now;System.err.println("[PZVR Physicality] Furniture contact skipped: "+unsupported);}}
   }
  }
  for(var faucet:WorldVisuals.contacts(player)){if(result.size()>=128)break;result.add(faucet);}sampleReady=capture&&WorldRigCapture.latest==sampledRig;return result;
 }
 public CapturedContact contact(IsoObject source){if(!sampleReady||sampleThread!=Thread.currentThread()||source==null)return null;var value=sampledSinks.get(source);return current(value)?value:null;}
 public boolean current(CapturedContact value){
  if(value==null||!sampleReady||sampleThread!=Thread.currentThread())return false;long now=System.nanoTime();var source=value.source();var square=value.square();var actor=value.actor();var rig=WorldRigCapture.latest;
  if(source==null||square==null||actor==null||actor.getSquare()==null||actor.getCell()==null||actor.getCell()!=value.cell.get()||actor.getCell().getGridSquare(square.getX(),square.getY(),square.getZ())!=square||IsoPlayer.players.length==0||IsoPlayer.players[0]!=actor||actor.isDead()||actor.getVehicle()!=null||zombie.GameTime.isGamePaused()||zombie.network.GameClient.client||zombie.network.GameServer.server||local.pzvr.VrApi.fullHud()||local.pzvr.VrApi.pointerActive()||zombie.ui.UIManager.isModalVisible()||zombie.core.Core.getInstance().isDoingTextEntry()||!Main.ready()||rig==null||rig.actor()!=actor||!rig.fresh(now)||rig.generation()!=value.generation||now<value.capturedAt||now-value.capturedAt>=150_000_000L||sampledSinks.get(source)!=value||source.getSquare()!=square||source.getObjectIndex()!=value.objectIndex||value.objectIndex<0||value.objectIndex>=square.getObjects().size()||square.getObjects().get(value.objectIndex)!=source||source.getSprite()!=value.sprite()||source.getSprite()==null||!Objects.equals(source.getSprite().name,value.name)||WorldMesher.rise(source)!=value.rise||!ChunkCache.packsOn(square)||value.model.state()!=PackModel.RESIDENT||PackedModelGeometry.get(value.model)!=value.mesh||!value.sinkProfile.matches(value.model.bounds()))return false;
  // Ordinary standalone sinks only. Do not consume ShelfBridge's expiring getter here: it may invalidate a native chunk.
  if(CounterTapBindings.owns(source)||sinkProfiles.checkedProfile(value.model)!=value.sinkProfile)return false;var bind=ModelPacks.bind(source.getSprite());if(bind!=value.binding||bind.model!=value.model||bind.x!=value.x||bind.y!=value.y||bind.z!=value.z||bind.scale!=value.scale)return false;boolean turns=PackBind.turns(source.getSprite());return (turns?bind.cos:1)==value.cos&&(turns?bind.sin:0)==value.sin;
 }
 public String status(){return "packed contacts "+exact+", missing meshes "+missing+", skipped "+skipped+"; "+(windows==null?"window contacts "+(windowChecked?"unavailable":"idle"):windows.status())+"; "+(generated==null?"generated contacts idle":generated.status());}
 public void clear(){sampleReady=false;sampledSinks.clear();sinkProfiles.clear();sampleThread=null;cache.clear();requestedCounters.clear();CounterTapBindings.contacts(this,false);if(windows!=null)windows.clear();if(generated!=null)generated.clear();windowChecked=false;exact=missing=skipped=0;}
}
