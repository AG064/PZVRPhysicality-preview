package pzvrphysicality;

import local.pzvr.*;import zombie.characters.IsoPlayer;import zombie.inventory.InventoryItem;import zombie.inventory.types.Food;import zombie.scripting.objects.ItemTag;

/** Builds native consumption intents. Lua and the original timed actions own every actual quantity change. */
public final class NativeConsumption {
 private static final ConsumePolicy<InventoryItem> policy=new ConsumePolicy<>();
 private static final UtensilPolicy<InventoryItem> utensils=new UtensilPolicy<>();
 private static volatile boolean enabled,requireTrigger=true;private static volatile double mouthDown=.11,radius=.105;
 private static IsoPlayer actor;private static long generation;private static String status="Physical eating and drinking Off";
 public static void configure(boolean value,boolean trigger,double down,double distance){
  if(!Double.isFinite(down)||down<.04||down>.2||!Double.isFinite(distance)||distance<.05||distance>.16)throw new IllegalArgumentException("Mouth calibration");
  enabled=value;requireTrigger=trigger;mouthDown=down;radius=distance;policy.reset();utensils.reset();UtensilCapture.clear();WorldRigCapture.requestUtensils(value);actor=null;generation++;
  status=value?"Physical eating/drinking armed; move items away from mouth first":"Physical eating and drinking Off";
 }
 private static IsoPlayer player(){return IsoPlayer.players.length==0?null:IsoPlayer.players[0];}
 /** NativeRig ordinary item slots remain physical left=secondary, right=primary. Preference only routes button roles. */
 static InventoryItem hand(IsoPlayer p,int side){return p==null||side<0||side>1?null:side==1?p.getPrimaryHandItem():p.getSecondaryHandItem();}
 private static boolean eligible(IsoPlayer p){return enabled&&p!=null&&!p.isDead()&&p.getVehicle()==null&&p.getSquare()!=null&&!zombie.network.GameClient.client&&!zombie.network.GameServer.server&&!zombie.GameTime.isGamePaused()&&!VrApi.fullHud()&&!VrApi.pointerActive()&&!zombie.ui.UIManager.isModalVisible()&&!zombie.core.Core.getInstance().isDoingTextEntry();}
 private static MouthMath.Pose pose(Math3.Pose pose){if(pose==null||pose.position()==null||pose.rotation()==null)return null;var p=pose.position();var q=pose.rotation();return new MouthMath.Pose(new MouthMath.Vec(p.x(),p.y(),p.z()),new MouthMath.Rotation(q.x(),q.y(),q.z(),q.w()));}
 private record Sample(ConsumePolicy.Kind kind,InventoryItem item,boolean near,boolean flow,boolean trigger){}
 private static ConsumePolicy.Kind kind(IsoPlayer p,InventoryItem item){
  if(item==null||item.getContainer()!=p.getInventory()||item.getWorldItem()!=null||!p.getInventory().contains(item)||item.isSealed())return null;
  var fluid=item.getFluidContainer();if(fluid!=null)return !fluid.isEmpty()&&fluid.canPlayerEmpty()&&fluid.getCapacity()<=3?ConsumePolicy.Kind.DRINK:null;
  if(!(item instanceof Food food)||!Float.isFinite(food.getHungChange())||food.getHungChange()>=0||food.isFrozen()||item.hasTag(ItemTag.SMOKABLE)||item.getOpeningRecipe()!=null&&!item.getOpeningRecipe().isBlank()
    ||item.getScriptItem()!=null&&Boolean.TRUE.equals(item.getScriptItem().isCantEat())||item.getRequireInHandOrInventory()!=null&&!item.getRequireInHandOrInventory().isEmpty())return null;
  return ConsumePolicy.Kind.FOOD;
 }
 private static Sample sample(IsoPlayer p,int side,long now){
  if(!eligible(p)||PzvrContacts.sample(now)==null)return null;var frame=Engine.latestInput;if(frame==null||frame.head()==null)return null;
  var hand=side==0?frame.left():frame.right();if(hand==null||!hand.tracked())return null;
  var item=hand(p,side);var kind=kind(p,item);if(kind==null||utensils.reserved(item))return null;
  try{
   var head=pose(frame.head());var held=pose(hand.grip());var profile=MouthMath.FOOD;
   if(kind==ConsumePolicy.Kind.DRINK){String type=(item.getEatType()==null?"":item.getEatType()).toLowerCase(java.util.Locale.ROOT);String full=item.getFullType().toLowerCase(java.util.Locale.ROOT);profile=type.contains("bottle")||full.contains("bottle")||full.contains("canteen")||full.contains("flask")?MouthMath.BOTTLE:MouthMath.CUP;}
   return new Sample(kind,item,MouthMath.contact(head,held,profile,mouthDown,radius),MouthMath.flowing(held,profile),hand.down(XrBackend.TRIGGER));
  }catch(IllegalArgumentException invalid){return null;}
 }
 public static boolean enabled(){return enabled;}
 public static double mouthRadius(){return radius;}
 public static boolean utensilEligible(IsoPlayer p,InventoryItem tool,InventoryItem food,int side){
  return side>=0&&side<=1&&eligible(p)&&tool!=null&&tool!=food&&UtensilCapture.supportedPair(tool,food)
   &&tool.getCondition()>0&&(tool.hasTag(ItemTag.SPOON)||tool.hasTag(ItemTag.FORK))&&tool.getContainer()==p.getInventory()
   &&tool.getWorldItem()==null&&!tool.isSealed()&&p.getInventory().contains(tool)&&hand(p,side)==tool
   &&hand(p,1-side)==food&&kind(p,food)==ConsumePolicy.Kind.FOOD;
 }
 private static Sample utensilSample(IsoPlayer p,int side,long now,boolean load){
  var tool=p==null?null:hand(p,side);var food=p==null?null:hand(p,1-side);var input=Engine.latestInput;
  var hand=input==null?null:side==0?input.left():input.right();var source=input==null?null:side==0?input.right():input.left();
  if(!utensilEligible(p,tool,food,side)||PzvrContacts.sample(now)==null||hand==null||source==null||!hand.tracked()||!source.tracked()||hand.grip()==null||source.grip()==null){
   if(load)utensils.cancel(side);return null;
  }
  var contact=UtensilCapture.sample(p,side,tool,food,now);
  if(contact==null){if(load)utensils.cancel(side);return null;}
  if(load){var f=(Food)food;var before=utensils.bite();var loaded=utensils.sample(side,tool,food,f.getHungChange(),f.getBaseHunger(),contact.foodContact(),contact.mouthContact(),true,p.hasTimedActions()||NativeItems.request()!=null||policy.busy()||!NativeItems.supportsPhysicalGestures(p),now);if(loaded!=null&&loaded!=before&&!UtensilCapture.loaded(p,loaded,now))utensils.cancel(side);}
  var bite=utensils.bite();if(bite==null||bite.side()!=side||bite.tool()!=tool||bite.food()!=food)return null;
  return new Sample(ConsumePolicy.Kind.UTENSIL,food,utensils.ready(side,tool,food)&&!contact.foodContact()&&contact.mouthContact(),true,requireTrigger&&hand.down(XrBackend.TRIGGER));
 }
 public static void tick(){
  if(!enabled)return;var p=player();if(p!=actor){policy.reset();utensils.reset();UtensilCapture.clear();actor=p;generation++;}
  long now=System.nanoTime();policy.reconcile(p!=null&&p.hasTimedActions(),now);var before=utensils.bite();
  if(before!=null&&utensils.ticket()>0&&!policy.owns(utensils.ticket(),before.side(),before.food()))utensils.finish(utensils.ticket());
  Sample[] contacts=new Sample[2];for(int side=0;side<2;side++)contacts[side]=utensilSample(p,side,now,true);
  if(before!=utensils.bite()&&utensils.bite()!=null)status="One utensil bite loaded; withdraw from food and approach the mouth";
  for(int side=0;side<2;side++){var s=contacts[side];if(s==null)s=sample(p,side,now);var request=policy.sample(side,s==null?null:s.item,s==null?null:s.kind,s!=null&&s.near,s!=null&&s.flow,s!=null&&s.trigger,s!=null,p!=null&&p.hasTimedActions()||NativeItems.request()!=null||!NativeItems.supportsPhysicalGestures(p),requireTrigger,now);if(request!=null)status=request.kind()==ConsumePolicy.Kind.DRINK?"Sip requested":request.kind()==ConsumePolicy.Kind.UTENSIL?"Utensil bite requested":"Bite requested";}
 }
 public static boolean ownsTrigger(int side){if(side<0||side>1)return false;long now=System.nanoTime();var p=player();var tool=p==null?null:hand(p,side);var food=p==null?null:hand(p,1-side);
  if(!NativeItems.supportsPhysicalGestures(p))return false;
  if(utensilEligible(p,tool,food,side)&&PzvrContacts.sample(now)!=null){var contact=UtensilCapture.sample(p,side,tool,food,now);if(contact!=null&&(contact.foodContact()||utensils.ready(side,tool,food)&&contact.mouthContact()))return true;}
  var s=sample(p,side,now);return s!=null&&s.near&&(s.kind==ConsumePolicy.Kind.FOOD||s.flow);
 }
 public static String routeTrigger(String key,String original){if(!enabled||!key.equals("dominant.trigger")&&!key.equals("other.trigger"))return original;return ConsumeRoute.resolve(ownsTrigger(0),ownsTrigger(1),VrApi.dominantHandIndex(),key,original);}
 public static boolean suppressAutoDrink(Object p){return enabled&&ConsumeRoute.suppressAuto(true,Engine.active(),Engine.worldPresent,p!=null&&p==player(),zombie.network.GameClient.client||zombie.network.GameServer.server);}
 public static ConsumePolicy.Request<InventoryItem> request(){return policy.request();}
 public static long generation(){return generation;}
 public static String status(){return enabled?status+"; "+UtensilCapture.status():status;}
 public static double mouthDown(){return mouthDown;}
 public static boolean requireTrigger(){return requireTrigger;}
 public static InventoryItem tool(){var bite=utensils.bite();return bite==null?null:bite.tool();}
 public static UtensilPolicy.Bite<InventoryItem> bite(){return utensils.bite();}
 public static boolean acknowledge(long ticket,boolean queued){boolean requested=queued;var request=policy.request();boolean utensil=request!=null&&request.ticket()==ticket&&request.kind()==ConsumePolicy.Kind.UTENSIL;
  if(queued&&!NativeItems.supportsPhysicalGestures(player()))queued=false;
  if(utensil&&queued&&!utensils.bind(ticket,request.side(),request.item()))queued=false;
  boolean acknowledged=policy.acknowledge(ticket,queued,System.nanoTime());if(utensil&&(!queued||!acknowledged))utensils.cancel(request.side());return acknowledged&&(!requested||queued)&&(!utensil||queued);
 }
 public static double portion(){var r=request();if(r==null)return 0;if(r.kind()==ConsumePolicy.Kind.UTENSIL){var bite=utensils.bite();return bite!=null&&bite.side()==r.side()&&bite.food()==r.item()?bite.portion():0;}return r.kind()==ConsumePolicy.Kind.FOOD?MouthMath.biteFraction(((Food)r.item()).getBaseHunger()):MouthMath.sipFraction(r.item().getFluidContainer().getAmount());}
 public static boolean valid(IsoPlayer p,InventoryItem item,int side,long ticket,long epoch){
  if(epoch!=generation||p!=actor||!policy.owns(ticket,side,item))return false;long now=System.nanoTime();var bite=utensils.bite();
  if(bite!=null&&utensils.owns(ticket,side,bite.tool(),item)){
   var s=utensilSample(p,side,now,true);return s!=null&&s.item==item&&s.near&&(!requireTrigger||s.trigger);
  }
  var s=sample(p,side,now);return s!=null&&s.item==item&&s.near&&(!requireTrigger||s.trigger)&&(s.kind==ConsumePolicy.Kind.FOOD||s.flow);
 }
 public static void finish(long ticket){policy.finish(ticket,System.nanoTime());utensils.finish(ticket);}
 private NativeConsumption(){}
}
