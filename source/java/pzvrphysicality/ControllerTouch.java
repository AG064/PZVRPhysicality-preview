package pzvrphysicality;

import java.lang.reflect.Field;
import java.util.*;
import local.pzvr.ControllerProfiles;
import local.pzvr.Engine;
import local.pzvr.XrBackend;
import org.lwjgl.system.MemoryStack;

/** Passive channels in the upstream set, sampled after its one action sync. */
public final class ControllerTouch {
 public static final String PROFILE="/interaction_profiles/oculus/touch_controller";
 static final long MAX_AGE=250_000_000L;
 public enum Channel {
  TRIGGER_TOUCH("trigger/touch",false),STICK_TOUCH("thumbstick/touch",false),REST_TOUCH("thumbrest/touch",false),
  FACE_PRIMARY("",false),FACE_SECONDARY("",false),TRIGGER_VALUE("trigger/value",true),SQUEEZE_VALUE("squeeze/value",true);
  final String path;final boolean analog;
  Channel(String path,boolean analog){this.path=path;this.analog=analog;}
  String action(){return "pzvr_physicality_"+name().toLowerCase(Locale.ROOT);}
  String path(int side){String part=path;if(this==FACE_PRIMARY)part=(side==0?"x":"a")+"/touch";if(this==FACE_SECONDARY)part=(side==0?"y":"b")+"/touch";return "/user/hand/"+(side==0?"left":"right")+"/input/"+part;}
 }
 public record Contact(boolean available,boolean touched){public static final Contact UNKNOWN=new Contact(false,false);public Contact{if(!available)touched=false;}}
 public record Value(boolean available,float value){public static final Value UNKNOWN=new Value(false,0);public Value{if(!available||!Float.isFinite(value)||value<0||value>1){available=false;value=0;}}}
 public record Input(Contact trigger,Contact stick,Contact rest,Contact primary,Contact secondary,Value triggerValue,Value squeeze){
  public static final Input UNKNOWN=new Input(Contact.UNKNOWN,Contact.UNKNOWN,Contact.UNKNOWN,Contact.UNKNOWN,Contact.UNKNOWN,Value.UNKNOWN,Value.UNKNOWN);
  public Input{Objects.requireNonNull(trigger);Objects.requireNonNull(stick);Objects.requireNonNull(rest);Objects.requireNonNull(primary);Objects.requireNonNull(secondary);Objects.requireNonNull(triggerValue);Objects.requireNonNull(squeeze);}
 }
 public record Reading(Input input,long generation,long hostTime){public static final Reading UNKNOWN=new Reading(Input.UNKNOWN,0,0);}
 record Bound(Object action,String path){}
 /** Authored tests supply this transport; the default owns no extra XR lifecycle. */
 interface Transport {
  Object create(Context context,Channel channel,MemoryStack stack);
  int suggest(Context context,ControllerProfiles.Profile profile,List<Bound> bindings,MemoryStack stack);
  long profile(Context context,int side,MemoryStack stack);
  long path(Context context,String value,MemoryStack stack);
  Contact contact(Context context,Object action,int side,MemoryStack stack);
  Value value(Context context,Object action,int side,MemoryStack stack);
 }
 static final class Context {
  final Object backend,instance,session,set,lock;final long[] hands;final Map<?,?> originals;
  final boolean eligible;
  Context(Object backend,Object instance,Object session,Object set,Object lock,long[] hands,Map<?,?> originals,boolean eligible){this.backend=backend;this.instance=instance;this.session=session;this.set=set;this.lock=lock;this.hands=hands.clone();this.originals=originals;this.eligible=eligible;}
 }
 private static final class State {
  final Object set,session;final long[] hands;final EnumMap<Channel,Object> actions=new EnumMap<>(Channel.class);
  long generation;long profile;long[] sampledProfiles;boolean accepted,failed,attached;
  State(Context c){set=c.set;session=c.session;hands=c.hands.clone();generation=++sequence;}
 }
 private record Sample(Object backend,Object session,long generation,long host,long frameFloor,XrBackend.Hand left,XrBackend.Hand right,Input l,Input r){}
 private static final IdentityHashMap<Object,State> states=new IdentityHashMap<>();
 private static final ArrayDeque<Sample> samples=new ArrayDeque<>();private static long sequence;private static volatile String detail="Awaiting supported controller profile";
 private static volatile String sampleState="not sampled";private static long captured;
 private static Transport transport=new ControllerTouchXr();
 private static final ClassValue<Boolean> verified=new ClassValue<>(){protected Boolean computeValue(Class<?> type){try{if(!type.getName().equals("local.pzvr.OpenXrBackend"))return false;SdkPins.verify(type,SdkPins.PZVR);return true;}catch(Exception|LinkageError denied){return false;}}};
 private static Field field(Class<?> type,String name)throws ReflectiveOperationException{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 private static Context context(Object backend)throws ReflectiveOperationException {
  if(backend==null||!verified.get(backend.getClass()))throw new IllegalStateException("Unsupported OpenXR owner");
  var type=backend.getClass();Object session=field(type,"session").get(backend);
  Object presence=field(type,"presence").get(backend);boolean allows=allowsInput(presence);
  boolean eligible=session!=null&&field(type,"running").getBoolean(backend)&&!field(type,"lost").getBoolean(backend)&&field(type,"state").getInt(backend)==5&&field(type,"displayTime").getLong(backend)!=0&&allows;
  return new Context(backend,field(type,"instance").get(backend),session,field(type,"actionSet").get(backend),field(type,"inputLock").get(backend),(long[])field(type,"handPaths").get(backend),(Map<?,?>)field(type,"actions").get(backend),eligible);
 }
 static boolean allowsInput(Object presence)throws ReflectiveOperationException {if(presence==null)return false;var method=presence.getClass().getDeclaredMethod("allowsInput");method.setAccessible(true);return (boolean)method.invoke(presence);}
 public static boolean suggest(Object backend,ControllerProfiles.Profile profile,MemoryStack stack){
  if(profile==null||!PROFILE.equals(profile.id())||profile.extension()!=null&&!profile.extension().isEmpty())return false;
  try{return suggest(context(backend),profile,stack);}catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){detail="Touch registration unavailable; native bindings retained";return false;}
 }
 static synchronized boolean suggest(Context c,ControllerProfiles.Profile profile,MemoryStack stack){
  if(!PROFILE.equals(profile.id())||profile.extension()!=null&&!profile.extension().isEmpty()||c.instance==null||c.session==null||c.set==null||c.hands.length!=2||c.hands[0]==0||c.hands[1]==0||c.hands[0]==c.hands[1])return false;
  State s=states.get(c.backend);
  if(s==null||s.set!=c.set||s.session!=c.session){if(states.size()>=2){states.clear();samples.clear();}clearSample(c.backend);s=new State(c);states.put(c.backend,s);}
  if(s.failed||s.attached||!Arrays.equals(s.hands,c.hands))return false;
  try{
   for(Channel channel:Channel.values())if(!s.actions.containsKey(channel)){Object action=transport.create(c,channel,stack);if(action==null)throw new IllegalStateException("Inactive touch action");s.actions.put(channel,action);}
   var union=new ArrayList<Bound>();for(var binding:profile.bindings()){Object action=c.originals.get(binding.action());if(action==null)throw new IllegalStateException("Missing native binding action");union.add(new Bound(action,binding.path()));}
   for(Channel channel:Channel.values())for(int side=0;side<2;side++)union.add(new Bound(s.actions.get(channel),channel.path(side)));
   long path=transport.path(c,PROFILE,stack);if(path==0)throw new IllegalStateException("Unavailable interaction profile");
   int result=transport.suggest(c,profile,List.copyOf(union),stack);
   if(result!=0){s.failed=true;s.accepted=false;clearSample(c.backend);detail="Touch suggestion rejected; native bindings retained";return false;}
   s.profile=path;s.accepted=true;detail="Classic Touch passive channels registered";return true;
  }catch(RuntimeException|LinkageError unavailable){s.failed=true;s.accepted=false;clearSample(c.backend);detail="Touch registration unavailable; native bindings retained";return false;}
 }
 /** Observes completion of the original attachment. Never attaches, creates or syncs an action set. */
 public static void actionsCreated(Object backend,Throwable failure){
  try{actionsCreated(context(backend),failure==null);}catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){clearSample(backend);sampleState="native attachment observation unavailable";}
 }
 static synchronized void actionsCreated(Context c,boolean success){
  var s=states.get(c.backend);if(s==null){if(c.session==null||c.set==null)return;if(states.size()>=2){states.clear();samples.clear();}s=new State(c);states.put(c.backend,s);}
  if(s.set!=c.set||s.session!=c.session||c.session==null||!Arrays.equals(s.hands,c.hands)){clearSample(c.backend);sampleState="native attachment identity changed";return;}
  s.attached=success;if(!success){s.failed=true;clearSample(c.backend);sampleState="native action creation/attachment failed";}else detail=s.accepted?"Classic Touch passive channels registered and native set attached":"Native set attached; passive bindings unavailable";
 }
 public static void capture(Object backend,MemoryStack stack,long xrTime,XrBackend.Hand[] hands){
  try{capture(context(backend),stack,xrTime,hands,System.nanoTime());}catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){clearSample(backend);sampleState="capture unavailable: "+unavailable.getClass().getSimpleName();}
 }
 static synchronized void capture(Context c,MemoryStack stack,long xrTime,XrBackend.Hand[] hands,long now){
  State s=states.get(c.backend);
  if(s==null||!s.accepted||!s.attached||s.set!=c.set||s.session!=c.session||!Arrays.equals(s.hands,c.hands)||!c.eligible||!Thread.holdsLock(c.lock)||xrTime==0||hands==null||hands.length!=2||hands[0]==hands[1]){clearSample(c.backend);sampleState=s==null||!s.accepted?"passive actions not registered":s.set!=c.set||s.session!=c.session?"action set/session changed":!Arrays.equals(s.hands,c.hands)?"declared subaction paths changed":!s.attached?"native set attachment not observed":!c.eligible?"session input ineligible":!Thread.holdsLock(c.lock)?"upstream input lock absent":"native hand sample invalid";return;}
  try{
   long[] profiles={transport.profile(c,0,stack),transport.profile(c,1,stack)};
   if(s.sampledProfiles!=null&&!Arrays.equals(s.sampledProfiles,profiles))clearSample(c.backend);s.sampledProfiles=profiles;
   Input[] inputs=new Input[2];for(int side=0;side<2;side++){var hand=hands[side];inputs[side]=hand!=null&&hand.tracked()&&profiles[side]==s.profile?read(c,s,side,stack):Input.UNKNOWN;}
   if(samples.size()>=6)samples.removeFirst();samples.addLast(new Sample(c.backend,c.session,s.generation,now,now,hands[0],hands[1],inputs[0],inputs[1]));captured++;sampleState="captured supported="+(profiles[0]==s.profile)+"/"+(profiles[1]==s.profile);
  }catch(RuntimeException|LinkageError unavailable){clearSample(c.backend);detail="Touch sample unavailable; native finger pose retained";sampleState="capture failed: "+unavailable.getClass().getSimpleName();}
 }
 private static Input read(Context c,State s,int side,MemoryStack stack){return new Input(transport.contact(c,s.actions.get(Channel.TRIGGER_TOUCH),side,stack),transport.contact(c,s.actions.get(Channel.STICK_TOUCH),side,stack),transport.contact(c,s.actions.get(Channel.REST_TOUCH),side,stack),transport.contact(c,s.actions.get(Channel.FACE_PRIMARY),side,stack),transport.contact(c,s.actions.get(Channel.FACE_SECONDARY),side,stack),transport.value(c,s.actions.get(Channel.TRIGGER_VALUE),side,stack),transport.value(c,s.actions.get(Channel.SQUEEZE_VALUE),side,stack));}
 public static Input input(XrBackend.Frame frame,int side,long now){return reading(frame,side,now).input();}
 public static synchronized Reading reading(XrBackend.Frame frame,int side,long now){
  if(frame==null||!frame.focused()||side<0||side>1)return Reading.UNKNOWN;
  var hand=side==0?frame.left():frame.right();if(hand==null||!hand.tracked())return Reading.UNKNOWN;
  for(var iterator=samples.descendingIterator();iterator.hasNext();){var s=iterator.next();var state=states.get(s.backend);
   if(frame.left()!=s.left||frame.right()!=s.right)continue;
   if(state==null||state.session!=s.session||state.generation!=s.generation||now<s.host||now-s.host>MAX_AGE||frame.time()<s.frameFloor||frame.time()>now)return Reading.UNKNOWN;
   return new Reading(side==0?s.l:s.r,s.generation,s.host);
  }return Reading.UNKNOWN;
 }
 private static synchronized void clearSample(Object backend){samples.removeIf(s->s.backend==backend);var state=states.get(backend);if(state!=null)state.generation=++sequence;}
 public static synchronized void close(Object backend){states.remove(backend);clearSample(backend);detail="Touch backend closed";sampleState="backend closed";}
 public static synchronized String status(){long registered=states.values().stream().filter(s->s.accepted).count(),attached=states.values().stream().filter(s->s.attached).count();return detail+"; registered="+registered+" attached="+attached+"; native readHands sampling="+sampleState+" count="+captured+"; "+diagnostics(Engine.latestInput,System.nanoTime());}
 public static String diagnostics(XrBackend.Frame frame,long now){return "L["+describe(reading(frame,0,now).input(),true)+"] R["+describe(reading(frame,1,now).input(),false)+"]";}
 private static String contact(Contact value){return !value.available()?"?":value.touched()?"touch":"clear";}
 private static String value(Value value){return !value.available()?"?":String.format(Locale.ROOT,"%.2f",value.value());}
 private static String describe(Input input,boolean left){return "trigger="+contact(input.trigger())+" stick="+contact(input.stick())+" rest="+contact(input.rest())+" "+(left?"X":"A")+"="+contact(input.primary())+" "+(left?"Y":"B")+"="+contact(input.secondary())+" triggerValue="+value(input.triggerValue())+" squeeze="+value(input.squeeze());}
 static synchronized void transportForTest(Transport authored){states.clear();samples.clear();transport=Objects.requireNonNull(authored);sampleState="not sampled";captured=0;}
 private ControllerTouch(){}
}
