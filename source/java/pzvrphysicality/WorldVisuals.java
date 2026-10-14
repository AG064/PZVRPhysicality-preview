package pzvrphysicality;
import java.util.*;import zombie.characters.IsoPlayer;import viewpointvr.physics.RigidPhysics;

/** Optional world additions with exact native model lineage and the existing tracked rig lease. */
public final class WorldVisuals {
 private static volatile boolean enabled;private static Thread thread;private static IsoPlayer actor;private static long generation=-1,nextDiscovery;private static boolean invalidate;
 private static final NearbyTaps nearby=new NearbyTaps();private static List<NearbyTaps.Target> targets=List.of();
 private static final Object depthDemand=new Object();
 public static volatile FaucetVisuals faucets=FaucetVisuals.EMPTY;private static volatile long drawnAt,drawnGeneration=-1;private static volatile List<FaucetVisuals.Row> drawnRows=List.of();private static String detail="World visuals Off";
 public static void configure(boolean value){enabled=value;thread=null;actor=null;targets=List.of();faucets=FaucetVisuals.EMPTY;nearby.clear();generation=-1;drawnAt=0;drawnGeneration=-1;drawnRows=List.of();nextDiscovery=0;invalidate=value;refreshRequests();WorldRigCapture.requestWorldVisuals(value);AuthoredDepth.request(depthDemand,value);detail=value?"World visuals waiting for exact emitted counter sinks":"World visuals Off";}
 public static void refreshRequests(){CounterTapBindings.request(enabled||NativeTaps.enabled());}
 public static boolean enabled(){return enabled;}
 private static IsoPlayer player(){return IsoPlayer.players.length==0?null:IsoPlayer.players[0];}
 static boolean eligible(IsoPlayer p,WorldRigFrame rig,long now){return enabled&&Main.ready()&&p!=null&&p==player()&&rig!=null&&rig.actor()==p&&rig.fresh(now)&&!p.isDead()&&p.getSquare()!=null&&p.getCell()!=null&&p.getVehicle()==null&&!zombie.network.GameClient.client&&!zombie.network.GameServer.server&&!zombie.GameTime.isGamePaused()&&!local.pzvr.VrApi.fullHud()&&!local.pzvr.VrApi.pointerActive()&&!zombie.ui.UIManager.isModalVisible()&&!zombie.core.Core.getInstance().isDoingTextEntry();}
 public static void tick(){if(!enabled)return;if(thread!=null&&thread!=Thread.currentThread())return;var p=player();var rig=WorldRigCapture.latest;long now=System.nanoTime();if(!eligible(p,rig,now)||WorldVisualRenderHook.failed()){targets=List.of();faucets=FaucetVisuals.EMPTY;drawnAt=0;detail="World visuals waiting for focused tracked world output";return;}if(thread==null)thread=Thread.currentThread();
  if(p!=actor||generation!=rig.generation()){actor=p;generation=rig.generation();targets=List.of();CounterTapBindings.clear();nextDiscovery=0;invalidate=true;drawnAt=0;}
  if(invalidate){invalidate=false;var here=p.getSquare();for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){var square=p.getCell().getGridSquare(here.getX()+dx,here.getY()+dy,here.getZ());if(square!=null)square.invalidateRenderChunkLevel(256L);}}
  if(now>=nextDiscovery){targets=nearby.sample(p,rig).stream().filter(t->t.profile().authored()).toList();nextDiscovery=now+200_000_000L;}
  var rows=new ArrayList<FaucetVisuals.Row>();for(var target:targets)if(NearbyTaps.current(target,p))rows.add(new FaucetVisuals.Row(target.base(),target.cos(),target.sin(),target.scale(),NativeTaps.open(target.source())));
  faucets=new FaucetVisuals(now,rig.generation(),rows);detail="World visuals: "+rows.size()+" exact authored counter faucets; native curtains/windows remain provider-owned";
 }
 static void drawn(long time,long rigGeneration){drawn(new FaucetVisuals(time,rigGeneration,faucets.rows()));}
 static void drawn(FaucetVisuals data){if(enabled&&faucets.time()==data.time()&&faucets.generation()==data.generation()){drawnRows=data.rows();drawnGeneration=data.generation();drawnAt=System.nanoTime();}}
 public static void rejectRenderReceipt(){drawnAt=0;drawnGeneration=-1;drawnRows=List.of();}
 private static FaucetVisuals.Row row(NearbyTaps.Target target,FaucetVisuals data){for(var row:data.rows())if(row.base().equals(target.base())&&row.cos()==target.cos()&&row.sin()==target.sin()&&row.scale()==target.scale())return row;return null;}
 public static FaucetVisuals forScene(Object scene,long now){var data=faucets;var rows=new ArrayList<FaucetVisuals.Row>();if(!enabled||!data.fresh(now))return FaucetVisuals.EMPTY;for(var target:targets)if(NearbyTaps.current(target,player())&&CounterTapBindings.rendered(target.owner(),scene,now)){var row=row(target,data);if(row!=null)rows.add(row);}return new FaucetVisuals(data.time(),data.generation(),rows);}
 public static boolean visible(NearbyTaps.Target target){long now=System.nanoTime();var rig=WorldRigCapture.latest;var data=faucets;var row=row(target,data);if(row==null||!drawnRows.contains(row)||!eligible(player(),rig,now)||drawnGeneration!=rig.generation()||now<drawnAt||now-drawnAt>=250_000_000L||!data.fresh(now)||!NearbyTaps.current(target,player())||!CounterTapBindings.rendered(target.owner(),rig.scene(),now))return false;return targets.stream().anyMatch(t->t.source()==target.source()&&t.owner()==target.owner()&&t.model()==target.model()&&t.base().distance(target.base())<.0001&&t.cos()==target.cos()&&t.sin()==target.sin()&&t.scale()==target.scale());}
 public static List<PhysicalWorld.Static> contacts(IsoPlayer p){if(!enabled||thread!=Thread.currentThread()||p!=actor)return List.of();var rows=new ArrayList<PhysicalWorld.Static>();int index=0;var data=faucets;for(var target:targets)if(visible(target)){var row=row(target,data);if(row==null)continue;var shape=FaucetMesh.shape(target.profile(),row.open(),target.cos(),target.sin(),target.scale());rows.add(new PhysicalWorld.Static("faucet:"+target.owner().getSquare().getX()+":"+target.owner().getSquare().getY()+":"+target.owner().getObjectIndex()+":"+(index++),shape,RigidPhysics.pose((float)target.base().x(),(float)target.base().y(),(float)target.base().height())));}return List.copyOf(rows);}
 public static String status(){long now=System.nanoTime();boolean fresh=enabled&&drawnAt>0&&now>=drawnAt&&now-drawnAt<250_000_000L&&faucets.fresh(now);return detail+"; final rendered receipt="+(fresh?"fresh":drawnAt>0?"expired":"none")+"; no smooth sash animation or extra wall thickness";}
 private WorldVisuals(){}
}
