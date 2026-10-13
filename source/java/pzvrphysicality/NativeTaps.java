package pzvrphysicality;

import java.util.*;import local.pzvr.*;import zombie.characters.IsoPlayer;import zombie.inventory.InventoryItem;import zombie.iso.IsoObject;

/** Calibrated hand interaction and native water-action requests. Never creates fluid or alters player statistics. */
public final class NativeTaps {
 public record Request(long ticket,int side,IsoPlayer actor,IsoObject source,InventoryItem cup,long epoch,long created){}
 private static final NearbyTaps nearby=new NearbyTaps();private static final TapGesture<IsoObject> valves=new TapGesture<>();
 private static List<NearbyTaps.Target> targets=List.of();private static volatile boolean enabled;
 private static IsoPlayer actor;private static long epoch,rigGeneration=-1,nextDiscovery,sequence,activeSince,retryAt;private static Request pending,active;
 private static String status="Physical taps Off";
 private static final boolean[] neutral=new boolean[2];
 public static volatile TapVisuals visuals=TapVisuals.EMPTY;
 public static void configure(boolean value){enabled=value;reset();WorldRigCapture.request(value);status=value?"Physical taps armed; release triggers before touching a handle":"Physical taps Off";}
 private static void reset(){targets=List.of();valves.reset();Arrays.fill(neutral,false);nearby.clear();actor=null;pending=active=null;visuals=TapVisuals.EMPTY;epoch++;rigGeneration=-1;nextDiscovery=retryAt=0;}
 private static IsoPlayer player(){return IsoPlayer.players.length==0?null:IsoPlayer.players[0];}
 private static boolean eligible(IsoPlayer p,WorldRigFrame rig,long now){return enabled&&p!=null&&rig!=null&&rig.actor()==p&&rig.fresh(now)&&!p.isDead()&&p.getSquare()!=null&&p.getVehicle()==null&&!zombie.network.GameClient.client&&!zombie.network.GameServer.server&&!zombie.GameTime.isGamePaused()&&!VrApi.fullHud()&&!VrApi.pointerActive()&&!zombie.ui.UIManager.isModalVisible()&&!zombie.core.Core.getInstance().isDoingTextEntry();}
 private static boolean current(NearbyTaps.Target target,IsoPlayer p){
  var source=target.source();var square=source.getSquare();if(square==null||source.getObjectIndex()<0||p.getSquare()==null||!square.isAdjacentTo(p.getSquare())||square.isBlockedTo(p.getSquare())||source.getSprite()==null)return false;
  var bind=viewpoint.packs.ModelPacks.bind(source.getSprite());if(bind==null||bind.model!=target.model()||!target.profile().matches(bind.model.bounds()))return false;
  var base=new ContactFrame.Point(square.getX()+.5+bind.x,square.getY()+.5+bind.y,square.getZ()*Math.sqrt(6)+viewpoint.world.WorldMesher.rise(source)+bind.z);
  boolean turn=viewpoint.packs.PackBind.turns(source.getSprite());float c=turn?bind.cos:1,s=turn?bind.sin:0;
  return base.distance(target.base())<.0001&&Math.abs(c-target.cos())<.0001&&Math.abs(s-target.sin())<.0001&&Math.abs(bind.scale-target.scale())<.0001;
 }
 private static NearbyTaps.Target nearestHandle(IsoPlayer p,WorldRigFrame.Hand hand){
  if(NativeItems.hand(p,hand.side())!=null)return null;NearbyTaps.Target closest=null;double distance=.12*hand.units();
  for(var target:targets){if(!current(target,p))continue;for(var point:target.handles()){double d=point.distance(hand.grip());if(d<distance){distance=d;closest=target;}}}return closest;
 }
 private static MouthMath.Profile cupProfile(InventoryItem item){String type=(item.getEatType()==null?"":item.getEatType()).toLowerCase(Locale.ROOT),name=item.getFullType().toLowerCase(Locale.ROOT);return type.contains("bottle")||name.contains("bottle")||name.contains("canteen")||name.contains("flask")?MouthMath.BOTTLE:MouthMath.CUP;}
 private static InventoryItem fillable(IsoPlayer p,WorldRigFrame.Hand hand){
  var item=NativeItems.hand(p,hand.side());if(item==null||item.getContainer()!=p.getInventory()||item.getWorldItem()!=null||item.isSealed()||item.getFluidContainer()==null||item.getFluidContainer().getFreeCapacity()<=0||hand.up().height()/hand.units()<.5)return null;return item;
 }
 private static boolean touchingStream(NearbyTaps.Target target,IsoPlayer p,WorldRigFrame rig,WorldRigFrame.Hand hand,InventoryItem cup){
  if(!current(target,p)||!valves.open(target.source())||!target.source().hasFluid())return false;
  if(cup!=null){if(fillable(p,hand)!=cup||!target.source().canTransferFluidTo(cup.getFluidContainer()))return false;return TapGesture.atStream(true,target.spout(),hand.point(cupProfile(cup).contact()),true,hand.units());}
  return neutral[hand.side()]&&NativeItems.hand(p,hand.side())==null&&TapGesture.atStream(true,target.spout(),rig.mouth(),false,hand.units())&&(!NativeConsumption.requireTrigger()||hand.trigger());
 }
 public static void tick(){
  if(!enabled)return;var p=player();var rig=WorldRigCapture.latest;long now=System.nanoTime();
  if(TapRenderHook.failed()){configure(false);status="Physical taps disabled: visual output unavailable; see console log";return;}
  if(p!=actor){reset();actor=p;}
  if(!eligible(p,rig,now)){pending=null;visuals=TapVisuals.EMPTY;Arrays.fill(neutral,false);for(int side=0;side<2;side++)valves.sample(side,null,false,false,0,false);return;}
  if(rigGeneration!=rig.generation()){targets=List.of();pending=active=null;valves.reset();Arrays.fill(neutral,false);epoch++;rigGeneration=rig.generation();nextDiscovery=0;}
  if(now>=nextDiscovery){targets=nearby.sample(p,rig);nextDiscovery=now+200_000_000L;}
  if(pending!=null&&now-pending.created()>500_000_000L)pending=null;
  if(active!=null&&!p.hasTimedActions()&&now-activeSince>250_000_000L){active=null;retryAt=now+250_000_000L;}
  for(var hand:rig.hands()){
   if(!hand.trigger())neutral[hand.side()]=true;
   var target=nearestHandle(p,hand);var change=valves.sample(hand.side(),target==null?null:target.source(),target!=null,hand.trigger(),hand.turnDegrees(),true);
   if(change!=TapGesture.Change.NONE)status=change==TapGesture.Change.OPENED?"Tap open":"Tap closed";
   if(pending!=null||active!=null||p.hasTimedActions()||NativeItems.request()!=null||NativeConsumption.request()!=null||now<retryAt)continue;
   var cup=fillable(p,hand);for(var source:targets)if(touchingStream(source,p,rig,hand,cup)){
    pending=new Request(++sequence,hand.side(),p,source.source(),cup,epoch,now);status=cup==null?"Tap sip requested":"Cup fill requested";break;
   }
  }
  var dots=new ArrayList<TapVisuals.Dot>();var streams=new ArrayList<TapVisuals.Stream>();double units=rig.hands().getFirst().units();
  for(var target:targets){if(!current(target,p))continue;for(var point:target.handles())dots.add(new TapVisuals.Dot(point,rig.hands().stream().anyMatch(h->NativeItems.hand(p,h.side())==null&&h.grip().distance(point)<.12*h.units())));
   if(valves.open(target.source())&&target.source().hasFluid()){double bottom=target.base().height()+target.profile().bounds()[1]*target.scale()+.04*units;streams.add(new TapVisuals.Stream(target.spout(),new ContactFrame.Point(target.spout().x(),target.spout().y(),Math.min(target.spout().height()-.08*units,bottom)),units));}
  }
  visuals=new TapVisuals(now,rig.generation(),dots,streams);
 }
 public static Request request(){return pending;}
 public static boolean busy(){return pending!=null||active!=null;}
 public static boolean enabled(){return enabled;}
 public static boolean acknowledge(long ticket,boolean queued){long now=System.nanoTime();if(pending==null||pending.ticket()!=ticket||now-pending.created()>500_000_000L)return false;if(queued){active=pending;activeSince=now;}else retryAt=now+300_000_000L;pending=null;return true;}
 public static boolean valid(long ticket,long generation){
  var p=player();var rig=WorldRigCapture.latest;long now=System.nanoTime();if(active==null||active.ticket()!=ticket||active.epoch()!=generation||generation!=epoch||p!=active.actor()||!eligible(p,rig,now)||rig.generation()!=rigGeneration)return false;
  var hand=rig.hand(active.side());if(hand==null)return false;var target=targets.stream().filter(t->t.source()==active.source()).findFirst().orElse(null);return target!=null&&touchingStream(target,p,rig,hand,active.cup());
 }
 public static void finished(long ticket){if(active!=null&&active.ticket()==ticket){active=null;retryAt=System.nanoTime()+250_000_000L;}}
 public static boolean ownsTrigger(int side){var p=player();var rig=WorldRigCapture.latest;if(!eligible(p,rig,System.nanoTime())||rig.generation()!=rigGeneration)return false;var hand=rig.hand(side);if(hand==null)return false;if(nearestHandle(p,hand)!=null)return true;var cup=fillable(p,hand);return targets.stream().anyMatch(t->touchingStream(t,p,rig,hand,cup));}
 public static String routeTrigger(String key,String original){if(!enabled||!key.equals("dominant.trigger")&&!key.equals("other.trigger"))return original;return ConsumeRoute.resolve(ownsTrigger(0),ownsTrigger(1),VrApi.dominantHandIndex(),key,original);}
 public static String status(){return status+"; calibrated sources "+targets.size()+", unsupported water objects "+nearby.unsupported();}
 private NativeTaps(){}
}
