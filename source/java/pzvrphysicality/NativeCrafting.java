package pzvrphysicality;

import java.util.Set;import local.pzvr.*;import zombie.characters.IsoPlayer;import zombie.inventory.InventoryItem;

/** A physical entry to the original recipe-for-item UI, with no crafting implementation here. */
public final class NativeCrafting {
 private static final Set<String> TOOLS=Set.of("Base.Hammer","Base.Saw","Base.Screwdriver","Base.Scissors","Base.Wrench","Base.KitchenKnife");
 private static final CraftingPolicy<InventoryItem> policy=new CraftingPolicy<>();private static volatile boolean enabled;private static volatile double reach=.14;
 private static NativeActionAuthority.Stamp requestAuthority;
 private static IsoPlayer actor;private static long generation=1;private static String status="Physical crafting shortcut Off";
 public record Request(long ticket,int side,IsoPlayer actor,InventoryItem tool,long generation,long created){}
 private record Contact(InventoryItem tool,boolean touching,boolean trigger){}
 private static IsoPlayer player(){return IsoPlayer.players.length==0?null:IsoPlayer.players[0];}
 public static void configure(boolean value,double distance){if(!Double.isFinite(distance)||distance<.08||distance>.3)throw new IllegalArgumentException("Crafting hand reach");if(value&&!Main.ready())throw new IllegalStateException("SDK validation pending");enabled=value;reach=distance;actor=null;generation++;policy.reset();status=value?"Crafting armed; separate hands and release trigger first":"Physical crafting shortcut Off";}
 private static boolean eligible(IsoPlayer p){return enabled&&!NativeInputOwnership.globalBlocked()&&Main.ready()&&p!=null&&p==player()&&!p.isDead()&&p.getVehicle()==null&&p.getSquare()!=null&&Engine.active()&&Engine.worldPresent&&!zombie.network.GameClient.client&&!zombie.network.GameServer.server&&!zombie.GameTime.isGamePaused()&&!VrApi.fullHud()&&!VrApi.pointerActive()&&!zombie.ui.UIManager.isModalVisible()&&!zombie.core.Core.getInstance().isDoingTextEntry()&&!p.hasTimedActions()&&NativeItems.supportsPhysicalGestures(p);}
 private static boolean deferred(int side){return NativeItems.request()!=null||NativeConsumption.request()!=null||NativeConsumption.bite()!=null||NativeTaps.request()!=null||NativeConsumption.ownsTrigger(side)||NativeTaps.ownsTrigger(side)||NativeCurtains.busy()||NativeCurtains.reservesGrip(side)||NativeBody.ownsGrip(side);}
 private static Contact contact(IsoPlayer p,int side,long now){
  if(side<0||side>1||!NativeInputOwnership.allows(side)||!NativeInputOwnership.allows(1-side)||!eligible(p)||deferred(side)||NativeItems.hand(p,side)!=null)return null;var tool=NativeItems.hand(p,1-side);
  if(tool==null||!TOOLS.contains(tool.getFullType())||tool.getCondition()<=0||tool.getContainer()!=p.getInventory()||tool.getWorldItem()!=null||!p.getInventory().contains(tool))return null;
  var input=Engine.latestInput;if(input==null||!input.focused()||input.head()==null||PzvrContacts.sample(now)==null)return null;var empty=side==0?input.left():input.right();var held=side==0?input.right():input.left();
  if(empty==null||held==null||!empty.tracked()||!held.tracked()||empty.grip()==null||held.grip()==null||empty.grip().position()==null||held.grip().position()==null||empty.grip().rotation()==null||held.grip().rotation()==null)return null;
  try{var a=empty.grip().position();var b=held.grip().position();double distance=new MouthMath.Vec(a.x(),a.y(),a.z()).distance(new MouthMath.Vec(b.x(),b.y(),b.z()));if(input!=Engine.latestInput)return null;return new Contact(tool,distance<=reach,empty.down(XrBackend.TRIGGER));}catch(IllegalArgumentException invalid){return null;}
 }
 public static void tick(){try(var ownership=NativeInputOwnership.open()){if(!enabled)return;var p=player();if(p!=actor){actor=p;generation++;policy.reset();}long now=System.nanoTime();for(int side=0;side<2;side++){var c=contact(p,side,now);var r=policy.sample(side,c==null?null:c.tool,c!=null&&c.touching,c!=null&&c.trigger,c!=null,false,now);if(r!=null){requestAuthority=NativeActionAuthority.stamp(side,1-side);status="Native recipes requested for "+r.tool().getFullType();}}}}
 public static Request request(){var r=policy.request();return r==null?null:new Request(r.ticket(),r.side(),actor,r.tool(),generation,r.created());}
 public static boolean valid(IsoPlayer p,InventoryItem tool,int side,long ticket,long epoch){try(var ownership=NativeInputOwnership.open()){if(p!=actor||epoch!=generation||!NativeActionAuthority.current(requestAuthority))return false;long now=System.nanoTime();var c=contact(p,side,now);return c!=null&&c.tool==tool&&c.touching&&c.trigger&&policy.valid(ticket,side,tool,now);}}
 public static boolean acknowledge(long ticket,boolean opened){try(var ownership=NativeInputOwnership.open()){var request=policy.request();if(request==null||request.ticket()!=ticket)return false;boolean admitted=!opened||NativeActionAuthority.current(requestAuthority);boolean result=policy.acknowledge(ticket,opened&&admitted,System.nanoTime())&&admitted;requestAuthority=null;status=opened&&result?"Native crafting window opened; choose and confirm a recipe":"Crafting request retired; separate hands and release trigger";return result;}}
 public static void showHud(){if(Main.ready())VrApi.showHud();}
 public static boolean ownsTrigger(int side){try(var ownership=NativeInputOwnership.open()){var c=contact(player(),side,System.nanoTime());return c!=null&&c.touching;}}
 public static String routeTrigger(String key,String original){try(var ownership=NativeInputOwnership.open()){if(!enabled||!"dominant.trigger".equals(key)&&!"other.trigger".equals(key))return original;return ConsumeRoute.resolve(ownsTrigger(0),ownsTrigger(1),NativeInputOwnership.bindingHandIndex(),key,original);}}
 public static boolean enabled(){return enabled;}public static double reach(){return reach;}public static String status(){return status;}
 private NativeCrafting(){}
}
