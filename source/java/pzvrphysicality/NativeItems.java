package pzvrphysicality;

import java.util.*;
import local.pzvr.VrApi;
import local.pzvr.VrControls;
import viewpointvr.physics.*;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoWorldInventoryObject;
import viewpointvr.interaction.WorldMembership;

/** Native pickup/drop intent. The Lua timed actions own inventory transfers; this class never clones an item. */
public final class NativeItems {
 private static final double STOREY=Math.sqrt(6);
 private static final GrabLifecycle<InventoryItem> grabs=new GrabLifecycle<>();
 private static final ContactFrame.Point[] previous=new ContactFrame.Point[2];private static final long[] previousTime=new long[2];
 private static final double[][] velocity=new double[2][3];
 private static final IdentityHashMap<InventoryItem,IsoWorldInventoryObject> loose=new IdentityHashMap<>();
 private static final IdentityHashMap<InventoryItem,MoveableGeometry.Geometry> simulatedPacks=new IdentityHashMap<>();
 private static final ItemRestBudget<InventoryItem> resting=new ItemRestBudget<>();
 private static final IdentityHashMap<InventoryItem,Long> savedAt=new IdentityHashMap<>();
 private static Map<InventoryItem,float[]> savedVisuals=Map.of();private static long nextSavedScan;
 private static long nextShapePoll;
 private static PhysicalWorld<InventoryItem> physics;private static IsoPlayer actor;private static volatile long epoch=1;private static long lastStep,nextEnvironment;
 private static final FurnitureContacts furniture=new FurnitureContacts();
 private static final WorldMembership.Graph<IsoWorldInventoryObject,IsoGridSquare> membership=new WorldMembership.Graph<>(){
  public IsoGridSquare square(IsoWorldInventoryObject object){return object.getSquare();}public void square(IsoWorldInventoryObject object,IsoGridSquare square){object.setSquare(square);}
  public int index(IsoGridSquare square,IsoWorldInventoryObject object,boolean world){return world?square.getWorldObjects().indexOf(object):square.getObjects().indexOf(object);}
  public int size(IsoGridSquare square,boolean world){return world?square.getWorldObjects().size():square.getObjects().size();}
  public void remove(IsoGridSquare square,IsoWorldInventoryObject object,boolean world){if(world)square.getWorldObjects().remove(object);else square.getObjects().remove(object);}
  public void add(IsoGridSquare square,int index,IsoWorldInventoryObject object,boolean world){if(world)square.getWorldObjects().add(index,object);else square.getObjects().add(index,object);}
 };
 private static volatile boolean enabled;private static Request pending;private static String status="Physical floor items Off";
 private record Visual(java.lang.ref.WeakReference<IsoWorldInventoryObject> source,long time,long epoch,float[] pose){}
 private static volatile Map<InventoryItem,Visual> visualPoses=Map.of();
 public static float[] visualPose(InventoryItem item,IsoWorldInventoryObject source){
  var waiting=enabled?MoveableDropBootstrap.pose(item,source):null;if(waiting!=null)return waiting;var v=visualPoses.get(item);long now=System.nanoTime();return enabled&&v!=null&&v.epoch==epoch&&v.source.get()==source&&item.getWorldItem()==source&&now>=v.time&&now-v.time<150_000_000L?v.pose.clone():null;
 }
 public record Request(int kind,int side,InventoryItem item,IsoWorldInventoryObject source,IsoGridSquare square,ContactFrame.Point point,double[] velocity,long epoch,long created,zombie.inventory.ItemContainer container,float[] rotation,NativeStorage.Destination storage){public Request{velocity=velocity.clone();rotation=rotation.clone();}public double[] velocity(){return velocity.clone();}public float[] rotation(){return rotation.clone();}}
 public static InventoryItem hand(IsoPlayer p,int side){return p==null?null:side==1?p.getPrimaryHandItem():side==0?p.getSecondaryHandItem():null;}
 public static boolean enabled(){return enabled;}
 public static String guardStatus(){
  if(!enabled)return "Floor gesture Off";var p=IsoPlayer.players.length==0?null:IsoPlayer.players[0];
  if(p==null||p!=actor)return "Floor gesture awaiting current local actor";
  if(p.isDead()||p.getVehicle()!=null||p.getSquare()==null)return "Floor gesture actor unavailable";
  if(zombie.network.GameClient.client||zombie.network.GameServer.server)return "Floor gesture multiplayer guard";
  if(zombie.GameTime.isGamePaused())return "Floor gesture paused";
  if(VrApi.fullHud())return "Floor gesture full HUD owns input";
  if(VrApi.pointerActive())return "Floor gesture current pointer owns input";
  if(zombie.ui.UIManager.isModalVisible()||zombie.core.Core.getInstance().isDoingTextEntry())return "Floor gesture modal/text entry owns input";
  if(!supportsPhysicalGestures(p))return "Floor gesture native weapon/preview owns input";
  if(p.hasTimedActions())return "Floor gesture native timed action active";
  return "Floor gesture guard admitted; "+PzvrContacts.status();
 }
 public static boolean ownsGrip(int side){
  if(side<0||side>1)return false;var p=IsoPlayer.players.length==0?null:IsoPlayer.players[0];if(p==null||p!=actor||!eligible(p)||!supportsPhysicalGestures(p))return false;
  long now=System.nanoTime();var frame=PzvrContacts.sample(now);var h=frame==null?null:frame.hand(side);if(h==null)return false;
  var held=grabs.held(side);if(held!=null&&held==hand(p,side)&&held.getContainer()==p.getInventory()&&held.getWorldItem()==null&&p.getInventory().contains(held))return true;
  if(pending!=null&&pending.side()==side||grabs.hasPending(side,now))return true;
  if(p.hasTimedActions()||hand(p,side)!=null||p.getCell()==null)return false;
  return nearest(p,h)!=null||NativeStorage.nearest(h.centre(),now)!=null;
 }
 /** Ordinary gesture creation defers to upstream pistol support and foreign hand-model previews. */
 public static boolean supportsPhysicalGestures(IsoPlayer p){
  if(p==null)return false;var pistol=VrControls.supportedPistol;int side=VrControls.pistolHand;
  if(pistol!=null&&(side<0||side>1||hand(p,side)==pistol&&hand(p,1-side)==null))return false;
  var actions=p.getCharacterActions();if(actions==null||actions.isEmpty())return true;var action=actions.get(0);
  return action!=null&&(!action.overrideHandModels||action.getPrimaryHandItem()==p.getPrimaryHandItem()&&action.getSecondaryHandItem()==p.getSecondaryHandItem());
 }
 public static String status(){return status+"; "+furniture.status()+"; "+MeshRecovery.status()+"; "+GroundItemCapture.status()+(physics==null?"":"; "+physics.preparationStatus());}
 public static long epoch(){return epoch;}
 public static void configure(boolean next)throws Exception {
  if(enabled==next)return;
  if(next){RigidPhysics.loadDefault();reset();enabled=true;WorldRigCapture.requestStorage(true);status="Physical floor pickup/drop armed; release both grips first";}
  else{enabled=false;reset();WorldRigCapture.requestStorage(false);status="Physical floor items Off";}
 }
 private static void reset(){
  if(physics!=null)for(var entry:loose.entrySet()){var state=physics.state(entry.getKey());if(state!=null)NativeItemPlacement.save(entry.getKey(),entry.getValue(),state);}
  grabs.reset();MoveableCarry.clear();MoveableDropBootstrap.clear();pending=null;actor=null;epoch++;visualPoses=Map.of();savedVisuals=Map.of();nextSavedScan=nextShapePoll=0;resting.clear();savedAt.clear();Arrays.fill(previous,null);Arrays.fill(previousTime,0);loose.clear();simulatedPacks.clear();lastStep=nextEnvironment=0;furniture.clear();GroundItemCapture.clear();if(physics!=null){physics.close();physics=null;}
 }
 private static boolean eligible(IsoPlayer p){return enabled&&p!=null&&!p.isDead()&&p.getVehicle()==null&&p.getSquare()!=null&&!zombie.network.GameClient.client&&!zombie.network.GameServer.server&&!zombie.GameTime.isGamePaused()&&!VrApi.fullHud()&&!VrApi.pointerActive()&&!zombie.ui.UIManager.isModalVisible()&&!zombie.core.Core.getInstance().isDoingTextEntry();}
 private static IsoGridSquare destination(IsoPlayer p,ContactFrame.Point point){
  if(point==null||Math.abs(point.x()-p.getX())>2||Math.abs(point.y()-p.getY())>2)return null;
  int floor=(int)Math.floor(p.getZ());double height=point.height()/STOREY-floor;if(height<0||height>.9)return null;
  var square=p.getCell().getGridSquare((int)Math.floor(point.x()),(int)Math.floor(point.y()),floor);
  var current=p.getSquare();return square!=null&&square.TreatAsSolidFloor()&&current!=null&&current.canReachTo(square)?square:null;
 }
 private static IsoWorldInventoryObject nearest(IsoPlayer p,ContactFrame.Hand h){
  IsoWorldInventoryObject selected=null;double best=.28;var point=h.centre();int px=(int)Math.floor(p.getX()),py=(int)Math.floor(p.getY()),z=(int)Math.floor(p.getZ());
  for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){
   var square=p.getCell().getGridSquare(px+dx,py+dy,z);var current=p.getSquare();if(square==null||current==null||!current.canReachTo(square))continue;
   for(var object:square.getWorldObjects()){
    InventoryItem item=object.getItem();if(item==null||item.getWorldItem()!=object||item.getContainer()!=null)continue;
    var location=new ContactFrame.Point(object.getWorldPosX(),object.getWorldPosY(),object.getWorldPosZ()*STOREY);
    double distance=point.distance(location);if(distance<best){best=distance;selected=object;}
   }
  }return selected;
 }
 private static void sampleVelocity(ContactFrame.Hand h,long now){
  int side=h.side();var point=h.centre();long dt=now-previousTime[side];var before=previous[side];
  if(before!=null&&dt>0&&dt<150_000_000L){double seconds=dt/1e9;velocity[side][0]=Math.clamp((point.x()-before.x())/seconds,-12,12);velocity[side][1]=Math.clamp((point.y()-before.y())/seconds,-12,12);velocity[side][2]=Math.clamp((point.height()-before.height())/seconds,-12,12);}
  else Arrays.fill(velocity[side],0);previous[side]=point;previousTime[side]=now;
 }
 public static void tick(){
  if(!enabled)return;long now=System.nanoTime();IsoPlayer p=IsoPlayer.players.length==0?null:IsoPlayer.players[0];
  if(p!=actor){reset();actor=p;}
  var frame=PzvrContacts.sample(now);
  if(!eligible(p)||frame==null){for(int side=0;side<2;side++)grabs.sample(side,false,false,now);MoveableCarry.cancel();MoveableDropBootstrap.clear();pending=null;visualPoses=Map.of();Arrays.fill(previous,null);Arrays.fill(previousTime,0);lastStep=0;return;}
  if(pending!=null&&now-pending.created()>250_000_000L){grabs.cancel(pending.side());MoveableCarry.cancel();pending=null;}
  updatePhysics(p,now);boolean gestures=supportsPhysicalGestures(p);if(!gestures)status="Native pistol support or hand-model preview owns physical gestures";
  for(int side=0;side<2;side++){
   var h=frame.hand(side);if(h==null){grabs.sample(side,false,false,now);continue;}sampleVelocity(h,now);if(!gestures)continue;
   boolean body=RouteModes.body()&&NativeBody.ownsGrip(side)||RouteModes.curtains()&&NativeCurtains.reservesGrip(side)||RouteModes.backpack()&&BackpackContents.ownsGrip(side);boolean carrying=grabs.held(side)!=null&&grabs.held(side)==hand(p,side);var edge=grabs.sample(side,h.grip(),!body||carrying,now);if(body||pending!=null||p.hasTimedActions())continue;
   var held=grabs.held(side);if(held!=null&&held!=hand(p,side))grabs.released(side,held);
   if(edge==GrabLifecycle.Edge.GRAB&&hand(p,side)==null){
    var source=nearest(p,h);var stored=NativeStorage.nearest(h.centre(),now);
    if(stored!=null&&(source==null||stored.bounds().distance(h.centre())<h.centre().distance(new ContactFrame.Point(source.getWorldPosX(),source.getWorldPosY(),source.getWorldPosZ()*STOREY)))){
     if(grabs.reserve(side,stored.item(),now)){pending=new Request(3,side,stored.item(),null,stored.square(),h.centre(),velocity[side],epoch,now,stored.container(),new float[]{0,0,0,1},null);status="Native open-storage pickup selected";}
    }else if(source!=null&&grabs.reserve(side,source.getItem(),now)){MoveableCarry.reserve(p,source.getItem(),source,side,epoch);pending=new Request(1,side,source.getItem(),source,source.getSquare(),h.centre(),velocity[side],epoch,now,null,new float[]{0,0,0,1},null);status="Native floor pickup selected";}
   }
   else if(grabs.held(side)!=null&&edge==GrabLifecycle.Edge.RELEASE){
    var item=grabs.held(side);
    if(item!=hand(p,side)||item.getContainer()!=p.getInventory()||item.getWorldItem()!=null){grabs.released(side,item);continue;}
    var rig=WorldRigCapture.latest;var hand=rig!=null&&rig.actor()==p&&rig.fresh(now)?rig.hand(side):null;
    var rotation=hand==null?new float[]{0,0,0,1}:Arrays.copyOfRange(StoragePose.hand(hand),3,7);var stored=NativeStorage.destination(hand,item,now);
    if(stored!=null){pending=new Request(4,side,item,null,stored.square(),hand.grip(),velocity[side],epoch,now,stored.container(),rotation,stored);status="Native return-to-storage selected";}
    else{var square=destination(p,h.centre());if(square!=null){MoveableCarry.planDrop(p,item,side,epoch);pending=new Request(2,side,item,null,square,h.centre(),velocity[side],epoch,now,null,rotation,null);status="Native drop selected";}}
   }
  }
 }
 public static Request request(){return pending;}
 public static boolean gestureBusy(int side){return side>=0&&side<2&&(pending!=null&&pending.side()==side||grabs.held(side)!=null);}
 public static void adoptBody(IsoPlayer p,InventoryItem item,int side){var f=PzvrContacts.sample(System.nanoTime());var h=f==null?null:f.hand(side);if(enabled&&p==actor&&eligible(p)&&side>=0&&side<2&&h!=null&&hand(p,side)==item&&item.getContainer()==p.getInventory()&&p.getInventory().contains(item)&&item.getWorldItem()==null){grabs.reconcile(hand(p,0),hand(p,1));grabs.adopt(side,item,h.grip());}}
 public static void acknowledge(boolean queued){if(pending!=null&&!queued){grabs.cancel(pending.side());MoveableCarry.cancel();}pending=null;}
 public static boolean canReceive(IsoPlayer p,InventoryItem item,int side,long generation){var f=PzvrContacts.sample(System.nanoTime());return generation==epoch&&p==actor&&eligible(p)&&f!=null&&f.hand(side)!=null&&f.hand(side).grip()&&grabs.pending(side,item,System.nanoTime())&&hand(p,side)==null&&item.getContainer()==p.getInventory()&&item.getWorldItem()==null&&p.getInventory().contains(item);}
 public static void received(IsoPlayer p,InventoryItem item,int side,long generation){if(p==actor&&generation==epoch&&hand(p,side)==item&&item.getContainer()==p.getInventory()){if(grabs.confirm(side,item,System.nanoTime()))MoveableCarry.received(p,item,side,generation);NativeItemPlacement.clear(item);resting.remove(item);savedAt.remove(item);if(loose.remove(item)!=null&&physics!=null)physics.remove(item);simulatedPacks.remove(item);}}
 public static void dropped(IsoPlayer p,InventoryItem item,int side,long generation,double vx,double vy,double vh){
  dropped(p,item,side,generation,vx,vy,vh,0,0,0,1);
 }
 public static void dropped(IsoPlayer p,InventoryItem item,int side,long generation,double vx,double vy,double vh,float qx,float qy,float qz,float qw){
  if(p!=actor||generation!=epoch||!enabled||item.getContainer()!=null||item.getWorldItem()==null)return;
  var object=item.getWorldItem();if(object.getSquare()==null||!object.getSquare().getWorldObjects().contains(object))return;
  var capturedDrop=MoveableModels.packOnly(item)?MoveableCarry.dropped(p,item,side,generation):null;
  if(!MoveableModels.canSimulate(item)){if(loose.remove(item)!=null&&physics!=null)physics.remove(item);simulatedPacks.remove(item);grabs.released(side,item);resting.remove(item);savedAt.remove(item);NativeItemPlacement.clear(item);if(capturedDrop!=null)MoveableDropBootstrap.stage(p,item,object,capturedDrop,new float[]{(float)vx,(float)vy,(float)vh},generation);status="Native moveable dropped; pack-only models require full-pose rendering for physics";return;}
  if(physics==null)physics=new PhysicalWorld<>();if(loose.remove(item)!=null)physics.remove(item);simulatedPacks.remove(item);grabs.released(side,item);if(loose.size()>=24)return;
  float[] pose;CollisionShape shape;
  if(MoveableModels.packOnly(item)){var geometry=MoveableRenderHook.geometry(item);var drop=capturedDrop;if(geometry==null){status="Native moveable dropped; exact full-pose mesh unavailable";return;}shape=geometry.shape();pose=drop==null?MoveableRenderHook.groundPose(item,object):drop.pose();}
  else{float half=.04f;shape=CollisionShape.box(half,half,half,0,0,half);pose=new float[]{object.getWorldPosX(),object.getWorldPosY(),(float)(object.getWorldPosZ()*STOREY),qx,qy,qz,qw};}
  if(MoveableModels.packOnly(item)){if(!physics.addPrepared(item,shape,pose,Math.clamp(item.getActualWeight(),.01f,20))){var geometry=MoveableRenderHook.geometry(item);if(geometry!=null)MoveableDropBootstrap.stage(p,item,object,new MoveableCarry.Drop(geometry,pose),new float[]{(float)vx,(float)vy,(float)vh},generation);status="Native moveable drop waiting for exact mesh preparation";return;}}else physics.add(item,shape,pose,Math.clamp(item.getActualWeight(),.01f,50));
  physics.release(item,new float[]{(float)vx,(float)vy,(float)vh,0,0,0});loose.put(item,object);if(MoveableModels.packOnly(item))simulatedPacks.put(item,MoveableRenderHook.geometry(item));resting.wake(item);savedAt.remove(item);nextEnvironment=nextShapePoll=0;status="Native item dropped with gravity and available packed mesh contacts";
 }
 public static boolean stored(IsoPlayer p,InventoryItem item,int side,long generation,zombie.inventory.ItemContainer destination){
  if(p==actor&&generation==epoch&&enabled&&item.getContainer()==destination&&destination.contains(item)&&item.getWorldItem()==null){grabs.released(side,item);resting.remove(item);savedAt.remove(item);if(loose.remove(item)!=null&&physics!=null)physics.remove(item);simulatedPacks.remove(item);status="Original item returned to native storage";return true;}return false;
 }
 private static void savedNearby(IsoPlayer p,long now){
  if(now<nextSavedScan)return;nextSavedScan=now+500_000_000L;var poses=new IdentityHashMap<InventoryItem,float[]>();int examined=0;
  outer:for(int radius=0;radius<=3;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dy=-radius;dy<=radius;dy++){
   if(Math.abs(dx)!=radius&&Math.abs(dy)!=radius)continue;
   var square=p.getCell().getGridSquare((int)Math.floor(p.getX())+dx,(int)Math.floor(p.getY())+dy,(int)Math.floor(p.getZ()));if(square==null)continue;
   for(var source:square.getWorldObjects()){if(++examined>256)break outer;var item=source.getItem();if(loose.containsKey(item))continue;var saved=NativeItemPlacement.read(item,source);if(saved!=null)poses.put(item,saved.pose());}
  }savedVisuals=poses;
 }
 private static boolean samePack(InventoryItem item,MoveableGeometry.Geometry geometry){var prior=simulatedPacks.get(item);return prior!=null&&geometry!=null&&prior.source()==geometry.source()&&Arrays.equals(prior.rawToWorld(),geometry.rawToWorld());}
 private static boolean looseCurrent(InventoryItem item,IsoWorldInventoryObject object){return item.getWorldItem()==object&&item.getContainer()==null&&object.getSquare()!=null&&MoveableModels.canSimulate(item)&&(!MoveableModels.packOnly(item)||samePack(item,MoveableRenderHook.geometry(item)));}
 /** Rejected bodies must not participate in a collision step against still-valid items. */
 private static void pruneLoose(){for(var iterator=loose.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();if(looseCurrent(entry.getKey(),entry.getValue()))continue;var item=entry.getKey();physics.remove(item);simulatedPacks.remove(item);resting.remove(item);savedAt.remove(item);iterator.remove();}}
 private static void updatePhysics(IsoPlayer p,long now){
  savedNearby(p,now);
  for(var ready:MoveableDropBootstrap.ready(p,now)){if(loose.size()>=24)break;if(physics==null)physics=new PhysicalWorld<>();var item=ready.item();if(!physics.addPrepared(item,ready.geometry().shape(),ready.pose(),Math.clamp(item.getActualWeight(),.01f,20)))continue;if(!MoveableDropBootstrap.complete(ready)){physics.remove(item);continue;}var velocity=ready.velocity();physics.release(item,new float[]{velocity[0],velocity[1],velocity[2],0,0,0});loose.put(item,ready.source());simulatedPacks.put(item,ready.geometry());resting.wake(item);savedAt.remove(item);nextEnvironment=nextShapePoll=0;}
  var visual=new IdentityHashMap<InventoryItem,Visual>();
  for(var entry:savedVisuals.entrySet()){var source=entry.getKey().getWorldItem();if(source==null)continue;var pose=entry.getValue();if(Math.abs(pose[0]-source.getWorldPosX())<.01&&Math.abs(pose[1]-source.getWorldPosY())<.01&&Math.abs(pose[2]-source.getWorldPosZ()*STOREY)<.3)visual.put(entry.getKey(),new Visual(new java.lang.ref.WeakReference<>(source),now,epoch,pose));}
  if(physics==null){visualPoses=Collections.unmodifiableMap(visual);return;}pruneLoose();int z=(int)Math.floor(p.getZ());
  if(now>=nextShapePoll){nextShapePoll=now+100_000_000L;for(var entry:loose.entrySet()){
     if(!MoveableModels.canSimulate(entry.getKey()))continue;
    var moveable=MoveableModels.packOnly(entry.getKey())?MoveableRenderHook.geometry(entry.getKey()):null;
    if(moveable!=null&&!samePack(entry.getKey(),moveable))continue;
    var shape=moveable==null?GroundItemCapture.shape(entry.getKey(),entry.getValue(),epoch):moveable.shape();
   if(shape!=null&&physics.prepareAndReplace(entry.getKey(),shape,Math.clamp(entry.getKey().getActualWeight(),.01f,50)))resting.wake(entry.getKey());
  }}
  boolean awake=resting.awake()||loose.keySet().stream().anyMatch(item->!resting.known(item));
  if(now>=nextEnvironment){var environment=new ArrayList<PhysicalWorld.Static>();
  for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++){var sq=p.getCell().getGridSquare((int)Math.floor(p.getX())+dx,(int)Math.floor(p.getY())+dy,z);if(sq!=null&&sq.TreatAsSolidFloor())environment.add(new PhysicalWorld.Static(sq.getX()+":"+sq.getY()+":"+z,CollisionShape.box(.5f,.5f,.01f,0,0,-.01f),RigidPhysics.pose(sq.getX()+.5f,sq.getY()+.5f,(float)(z*STOREY))));}
  environment.addAll(furniture.sample(p));if(physics.environment(environment)){for(var item:loose.keySet())physics.wake(item);resting.clear();awake=true;}nextEnvironment=now+(awake?200_000_000L:1_000_000_000L);}
  pruneLoose();awake=resting.awake()||loose.keySet().stream().anyMatch(item->!resting.known(item));
  if(awake&&lastStep!=0)physics.step((float)Math.clamp((now-lastStep)/1e9,.001,.033));lastStep=now;
   for(var iterator=loose.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();var object=entry.getValue();var item=entry.getKey();if(!looseCurrent(item,object)){physics.remove(item);simulatedPacks.remove(item);resting.remove(item);savedAt.remove(item);iterator.remove();continue;}
   var state=resting.due(item,now)?physics.state(item):null;if(state!=null)resting.sample(item,state,now);
   var pose=resting.pose(item);var square=object.getSquare();if(pose==null)continue;
   var to=p.getCell().getGridSquare((int)Math.floor(pose[0]),(int)Math.floor(pose[1]),square.getZ());
   if(to==null||!to.TreatAsSolidFloor()||Math.abs(to.getX()-square.getX())>1||Math.abs(to.getY()-square.getY())>1||to!=square&&to.isBlockedTo(square)
     ||Math.abs(to.getX()-p.getX())>3||Math.abs(to.getY()-p.getY())>3){NativeItemPlacement.save(item,object,pose);physics.remove(item);simulatedPacks.remove(item);resting.remove(item);savedAt.remove(item);iterator.remove();continue;}
   float ox=object.getOffX(),oy=object.getOffY(),oz=object.getOffZ();
   float nx=pose[0]-to.getX(),ny=pose[1]-to.getY(),nz=Math.max(0,(float)(pose[2]/STOREY-to.getZ()));
   if((to!=square||Math.abs(ox-nx)>.0001||Math.abs(oy-ny)>.0001||Math.abs(oz-nz)>.0001)&&!WorldMembership.move(object,square,to,membership,()->object.setOffset(nx,ny,nz),()->object.setOffset(ox,oy,oz))){physics.remove(item);simulatedPacks.remove(item);resting.remove(item);savedAt.remove(item);iterator.remove();continue;}
   if(to!=square){square.invalidateRenderChunkLevel(256L);to.invalidateRenderChunkLevel(256L);}
   Long before=savedAt.get(item);if(before==null||now<before||now-before>=250_000_000L){NativeItemPlacement.save(item,object,pose);savedAt.put(item,now);}
   visual.put(item,new Visual(new java.lang.ref.WeakReference<>(object),now,epoch,Arrays.copyOf(pose,7)));
   if(to!=square&&MoveableModels.packOnly(item)){var geometry=simulatedPacks.get(item);var velocity=new float[]{state==null?0:state[7],state==null?0:state[8],state==null?0:state[9]};if(geometry!=null)MoveableDropBootstrap.stage(p,item,object,new MoveableCarry.Drop(geometry,Arrays.copyOf(pose,7)),velocity,epoch);physics.remove(item);simulatedPacks.remove(item);resting.remove(item);savedAt.remove(item);iterator.remove();}
  }
  visualPoses=Collections.unmodifiableMap(visual);
 }
 private NativeItems(){}
}
