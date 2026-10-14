package pzvrphysicality;
import java.util.*;
import local.pzvr.*;
import zombie.characters.IsoPlayer;

/** Optional avatar movement restriction. Native collision runs afterwards; headset poses are untouched. */
public final class NativePlayerCollision {
 interface Provider{List<PhysicalWorld.Static> sample(IsoPlayer actor);}private static Provider authoredProvider;
 static void provider(Provider value){authoredProvider=value;}
 private record Cached(viewpointvr.physics.CollisionShape shape,float[] pose,PlayerSweep.Mesh mesh,boolean planar){}
 private record Motion(java.lang.ref.WeakReference<IsoPlayer> actor,java.lang.ref.WeakReference<Object> cell,long time,double startX,double startY,double wantedX,double wantedY,boolean meshClipped,boolean meshAvailable){}
 private static final ThreadLocal<Integer> room=new ThreadLocal<>();
 private static final ThreadLocal<Motion> motion=new ThreadLocal<>();
 private static volatile boolean enabled;private static boolean roomScale=true;private static double radius=.18;private static Thread owner;private static Object actor,cell;private static long generation=-1;
 private static FurnitureContacts furniture;private static final Map<String,Cached> cache=new HashMap<>();
 private static long sampled,clipped,missing,budget,roomCueAt,nativeShort;private static int planar,depth;private static String detail="Player collision Off",nativeDetail="native movement not observed";
 public static synchronized void configure(boolean value,boolean physical,double centimetres){
  if(!Double.isFinite(centimetres)||centimetres<10||centimetres>35)throw new IllegalArgumentException("Player collision radius");
  enabled=false;RouteModes.playerCollision(false);NativeSinkCollision.reset();roomScale=physical;radius=centimetres/100;cache.clear();motion.remove();owner=null;actor=cell=null;generation=-1;sampled=clipped=missing=budget=roomCueAt=nativeShort=0;planar=depth=0;nativeDetail="native movement not observed";
  if(furniture!=null)furniture.clear();WorldRigCapture.requestPlayerCollision(value);WorldContactCapture.requestPlayerCollision(value);RouteModes.world(WorldContactCapture.enabled());
  enabled=value;RouteModes.playerCollision(value);detail=value?"Avatar collision armed; native headset lean remains unchanged":"Player collision Off";
 }
 public static boolean enabled(){return enabled;}public static boolean roomScaleEnabled(){return roomScale;}
 public static void roomEnter(){var depth=room.get();room.set(depth==null?1:Math.min(64,depth+1));}
 public static void roomExit(){var depth=room.get();if(depth==null||depth<=1)room.remove();else room.set(depth-1);}
 static boolean physical(){return room.get()!=null;}
 private static float[] planned(IsoPlayer p,float dx,float dy,boolean clipped,boolean available){var old=motion.get();long now=System.nanoTime();boolean same=old!=null&&old.actor.get()==p&&old.cell.get()==p.getCell()&&now>=old.time&&now-old.time<250_000_000L;motion.set(new Motion(new java.lang.ref.WeakReference<>(p),new java.lang.ref.WeakReference<>(p.getCell()),same?old.time:now,same?old.startX:p.getX(),same?old.startY:p.getY(),p.getNextX()+dx,p.getNextY()+dy,clipped||same&&old.meshClipped,available||same&&old.meshAvailable));return new float[]{dx,dy};}
 public static synchronized void observed(Object value,boolean completed){var r=motion.get();if(!(value instanceof IsoPlayer p)||r==null||r.actor.get()!=p)return;motion.remove();if(!completed||!enabled||r.cell.get()!=p.getCell()||System.nanoTime()-r.time>250_000_000L)return;
  try{double wx=r.wantedX-r.startX,wy=r.wantedY-r.startY,length=Math.hypot(wx,wy);if(length<.001)return;double actual=((p.getX()-r.startX)*wx+(p.getY()-r.startY)*wy)/length;if(actual+Math.max(.003,2*Math.ulp(p.getX()))<length){nativeShort++;var object=p.getCollidedObject();String sprite=object==null||object.getSprite()==null?"none":String.valueOf(object.getSprite().getName());if(sprite.length()>96)sprite=sprite.substring(0,96);String type=String.valueOf(p.getCollideType());if(type.length()>64)type=type.substring(0,64);nativeDetail="native shorter after "+(r.meshClipped?"mesh restriction":r.meshAvailable?"provided mesh query passed":"missing mesh omission")+"; type="+type+" door="+p.isCollidedWithDoor()+" vehicle="+p.isCollidedWithVehicle()+" object="+(object==null?"none":object.getClass().getSimpleName())+" sprite="+sprite;}else nativeDetail="native displacement completed after mesh query";
  }catch(RuntimeException|LinkageError unsupported){nativeDetail="native movement receipt unavailable";}
 }
 private static boolean context(IsoPlayer p,long now){var input=Engine.latestInput;var rig=WorldRigCapture.latest;
  return enabled&&Main.ready()&&p!=null&&IsoPlayer.players.length>0&&p==IsoPlayer.players[0]&&!p.isDead()&&p.getVehicle()==null&&p.getCell()!=null&&p.getSquare()!=null&&!p.isClimbing()&&!p.getSquare().HasStairs()&&!p.getSquare().HasStairsBelow()
   &&!p.hasTimedActions()&&!p.isBlockMovement()&&Engine.active()&&Engine.worldPresent&&!zombie.network.GameClient.client&&!zombie.network.GameServer.server&&!zombie.GameTime.isGamePaused()&&!VrApi.fullHud()&&!VrApi.pointerActive()&&!zombie.ui.UIManager.isModalVisible()&&!zombie.core.Core.getInstance().isDoingTextEntry()
   &&input!=null&&input.focused()&&input.head()!=null&&rig!=null&&rig.actor()==p&&rig.fresh(now)&&rig.inputTime()<=input.time()&&input.time()-rig.inputTime()<150_000_000L;
 }
 // A native tile refinement may borrow only the same owned, successful movement sample.
 static synchronized FurnitureContacts sinkContacts(IsoPlayer p,long now){var r=motion.get();var rig=WorldRigCapture.latest;
  if(!context(p,now)||physical()&&!roomScale||authoredProvider!=null||furniture==null||owner!=Thread.currentThread()||actor!=p||cell!=p.getCell()||rig==null||generation!=rig.generation()
   ||r==null||r.actor.get()!=p||r.cell.get()!=p.getCell()||!r.meshAvailable||now<r.time||now-r.time>250_000_000L
   ||Math.abs(p.getLastX()-r.startX)>.001||Math.abs(p.getLastY()-r.startY)>.001
   ||p.getNextX()<Math.min(r.startX,r.wantedX)-.001||p.getNextX()>Math.max(r.startX,r.wantedX)+.001||p.getNextY()<Math.min(r.startY,r.wantedY)-.001||p.getNextY()>Math.max(r.startY,r.wantedY)+.001)return null;
  return furniture;
 }
 static synchronized boolean sinkPathClear(IsoPlayer p,PhysicalWorld.Static row,double x,double y,double dx,double dy){var rig=WorldRigCapture.latest;
  if(rig==null||row==null||!context(p,System.nanoTime()))return false;var admitted=cache.get(row.key());if(admitted==null||admitted.shape!=row.shape()||!Arrays.equals(admitted.pose,row.pose()))return false;double units=rig.hands().isEmpty()?1:rig.hands().getFirst().units(),feet=p.getZ()*Math.sqrt(6);
  double height=Math.clamp(rig.mouth().height()+NativeConsumption.mouthDown()*units-feet,.55*units,2.2*units);
  boolean clear=PlayerSweep.clearPath(admitted.mesh,x,y,feet,height,radius*units,dx,dy);
  return clear&&WorldRigCapture.latest==rig&&context(p,System.nanoTime());
 }
 public static synchronized float[] move(Object value,float dx,float dy){
  var unchanged=new float[]{dx,dy};if(!enabled)return unchanged;if(!(value instanceof IsoPlayer p))return unchanged;
  if(physical()&&!roomScale){detail="Room-scale avatar restriction Off; native physical tracking retained";return unchanged;}
  long now=System.nanoTime();if(!context(p,now)){cache.clear();roomCueAt=0;planar=depth=0;detail="Player collision waiting for eligible tracked native movement";return unchanged;}
  if(!Float.isFinite(dx)||!Float.isFinite(dy)||Math.hypot(dx,dy)>.5){detail="Discontinuous native movement retained";return unchanged;}
  if(Math.hypot(dx,dy)<1e-7)return unchanged;
  var nextSquare=p.getCell().getGridSquare((int)Math.floor(p.getNextX()+dx),(int)Math.floor(p.getNextY()+dy),(int)Math.floor(p.getZ()));if(nextSquare!=null&&(nextSquare.HasStairs()||nextSquare.HasStairsBelow())){detail="Native stair approach retained";return unchanged;}
  if(owner!=null&&owner!=Thread.currentThread()){detail="Player collision omitted on foreign movement thread";return unchanged;}
  owner=Thread.currentThread();var rig=WorldRigCapture.latest;if(actor!=p||cell!=p.getCell()||generation!=rig.generation()){cache.clear();actor=p;cell=p.getCell();generation=rig.generation();}
  try{
   if(authoredProvider==null&&furniture==null)furniture=new FurnitureContacts();var supplied=authoredProvider==null?furniture.sample(p):authoredProvider.sample(p);var meshes=new ArrayList<PlayerSweep.Mesh>();var seen=new HashSet<String>();
   int storedTriangles=0;planar=depth=0;for(var row:supplied){if(meshes.size()>=PlayerSweep.MAX_SHAPES)break;int triangles=row.shape().faces().length/3;if(triangles==0||storedTriangles+triangles>60000){missing++;continue;}storedTriangles+=triangles;seen.add(row.key());var pose=row.pose();var old=cache.get(row.key());boolean stable=old!=null&&old.shape==row.shape()&&Arrays.equals(old.pose,pose);var mesh=stable?old.mesh:PlayerSweep.read(row.shape(),pose);boolean flat=stable?old.planar:mesh.planeDepth()<.001;cache.put(row.key(),new Cached(row.shape(),pose,mesh,flat));if(flat)planar++;else depth++;meshes.add(mesh);}cache.keySet().retainAll(seen);
   if(meshes.isEmpty()){missing++;detail="Player collision omitted: no current native mesh";return planned(p,dx,dy,false,false);}
   double units=rig.hands().isEmpty()?1:rig.hands().getFirst().units();double feet=p.getZ()*Math.sqrt(6),height=Math.clamp(rig.mouth().height()+NativeConsumption.mouthDown()*units-feet,.55*units,2.2*units);
   var result=PlayerSweep.move(meshes,p.getNextX(),p.getNextY(),feet,height,radius*units,dx,dy);sampled++;
   if(!context(p,System.nanoTime())||WorldRigCapture.latest!=rig){cache.clear();missing++;detail="Player collision omitted: world receipt advanced";return unchanged;}
   if(result.budget()){budget++;detail="Player collision omitted: triangle query budget";return planned(p,dx,dy,false,false);}if(result.clipped(dx,dy)){clipped++;if(physical())roomCueAt=System.nanoTime();}
   detail=(physical()?"Room-scale avatar":"Native locomotion avatar")+" uses "+meshes.size()+" actual mesh contacts; headset pose unchanged";return planned(p,(float)result.x(),(float)result.y(),result.clipped(dx,dy),true);
  }catch(RuntimeException|LinkageError unsupported){cache.clear();missing++;detail="Player collision omitted: "+unsupported.getClass().getSimpleName();return unchanged;}
 }
 public static synchronized String status(){return detail+"; enabled="+enabled+" roomScaleBody="+roomScale+" tested="+sampled+" clipped="+clipped+" missing="+missing+" budget="+budget+" planarMeshes="+planar+" depthMeshes="+depth+" nativeShort="+nativeShort+" "+nativeDetail+"; "+NativeSinkCollision.status()+(furniture==null?"":"; "+furniture.status());}
 public static synchronized double roomContactAlpha(){long now=System.nanoTime();return enabled&&roomScale&&roomCueAt>0&&now>=roomCueAt&&now-roomCueAt<500_000_000L?1-(now-roomCueAt)/500_000_000d:0;}
 public static String roomContactHint(){return roomContactAlpha()>0?"Avatar contact: step back or move along the surface; your headset can still lean through":"";}
 private NativePlayerCollision(){}
}
