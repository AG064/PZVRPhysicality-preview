package pzvrphysicality;
import java.util.*;import org.joml.Matrix4f;
/** Scoped, opt-in rendering for the proved native world-only phone; never changes inventory or actor model slots. */
public final class NativeHeldFallback {
 record Slot(int side,Object item){}
 record Capture(Object frame,Object actor,Object input,Object scene,Object skin,float[] base,int leftProp,int rightProp,List<Slot> slots,long time,long receipt,Object nativeState){Capture{base=base.clone();slots=List.copyOf(slots);}}
 record Anchor(Capture capture,long epoch,long host,long receipt,Thread owner,float[][] props){Anchor{props=new float[][]{props[0]==null?null:props[0].clone(),props[1]==null?null:props[1].clone()};}}
 record Drawer(int first,int last,float[] values,int[] flags,int[] palette,int[] shadow,float[] world,Object identity){Drawer{world=world.clone();}}
 interface Adapter {Capture begin(Object frame,Object slot,Object actor,long epoch);boolean current(Capture capture);long receipt();boolean eligible(Capture capture,Slot slot);Drawer draw(Capture capture,Slot slot);boolean drawerCurrent(Capture capture,Slot slot,Drawer drawer);void discard(Capture capture,Drawer drawer);default void contact(Capture capture,Slot slot,Drawer drawer,long epoch){}}
 private static Adapter adapter;private static Adapter access(){if(adapter==null)adapter=new NativeHeldAccess();return adapter;}
 private static volatile boolean enabled;private static volatile long epoch;private static volatile String detail="Native world-only held models Off";private static long published,drawn,rejected;
 private static final ThreadLocal<Deque<Scope>> scopes=new ThreadLocal<>();private static final IdentityHashMap<Object,Anchor> anchors=new IdentityHashMap<>();private static final Scope INACTIVE=new Scope();
 private static final class Scope{final Capture capture;final long epoch;final Thread owner;boolean valid=true;Object skin;Matrix4f[] output;Scope(){capture=null;epoch=-1;owner=null;valid=false;}Scope(Capture c,long e){capture=c;epoch=e;owner=Thread.currentThread();}}
 public static synchronized void configure(boolean value){boolean unavailable=value&&!Main.ready();enabled=value&&!unavailable;epoch++;anchors.clear();published=drawn=rejected=0;NativeHeldContact.configure(enabled);detail=unavailable?"Native world-only held models unavailable: SDK validation pending":value?"Native world-only held models armed; CordlessPhone only":"Native world-only held models Off";if(unavailable)throw new IllegalStateException("SDK validation pending");}
 public static boolean enabled(){return enabled;}public static synchronized String status(){return detail+"; anchors="+published+" drawn="+drawn+" rejected="+rejected+"; "+NativeHeldContact.status()+"; native models/grasps keep priority";}
 static boolean current(long generation,Capture capture){return enabled&&generation==epoch&&access().current(capture);}
 public static void begin(Object frame,Object slot,Object actor){var stack=scopes.get();if(!enabled&&stack==null)return;if(stack==null){stack=new ArrayDeque<>();scopes.set(stack);}Scope scope=INACTIVE;try{if(enabled){var capture=access().begin(frame,slot,actor,epoch);if(capture!=null)scope=new Scope(capture,epoch);}}catch(RuntimeException|LinkageError unavailable){detail="Native held anchor unavailable: "+unavailable.getClass().getSimpleName();}stack.push(scope);}
 public static void fingers(Object skin,Matrix4f[] output){var stack=scopes.get();var scope=stack==null?null:stack.peek();if(scope==null||scope==INACTIVE||!scope.valid||scope.owner!=Thread.currentThread()||scope.epoch!=epoch)return;if(scope.skin!=null&&(scope.skin!=skin||scope.output!=output)){scope.valid=false;return;}scope.skin=skin;scope.output=output;}
 public static void end(Throwable failure){var stack=scopes.get();if(stack==null||stack.isEmpty())return;var scope=stack.pop();try{
  if(failure!=null||scope==INACTIVE||!scope.valid||!enabled||scope.owner!=Thread.currentThread()||scope.epoch!=epoch||scope.skin!=scope.capture.skin()||scope.output==null||!access().current(scope.capture))return;long now=System.nanoTime(),receipt=access().receipt();if(receipt==scope.capture.receipt()||receipt<scope.capture.time()||now<scope.capture.time()||now-scope.capture.time()>150_000_000L)return;
  float[][] props=new float[2][];for(int side=0;side<2;side++){int index=side==0?scope.capture.leftProp():scope.capture.rightProp();if(index>=0&&index<scope.output.length&&scope.output[index]!=null)props[side]=scope.output[index].get(new float[16]);}
  synchronized(NativeHeldFallback.class){if(!enabled||scope.epoch!=epoch||!access().current(scope.capture))return;if(anchors.size()>=6&&!anchors.containsKey(scope.capture.frame()))anchors.clear();anchors.put(scope.capture.frame(),new Anchor(scope.capture,epoch,now,receipt,scope.owner,props));published++;detail="Successful native Prop2/Prop1 anchors captured";}
 }catch(RuntimeException|LinkageError unsupported){detail="Native held anchor rejected: "+unsupported.getClass().getSimpleName();}finally{if(stack.isEmpty())scopes.remove();}}
 public static void render(Object frame){if(!enabled)return;Anchor anchor;synchronized(NativeHeldFallback.class){anchor=anchors.remove(frame);}if(anchor==null)return;var capture=anchor.capture();long now=System.nanoTime();if(anchor.owner()!=Thread.currentThread()||anchor.epoch()!=epoch||now<anchor.host()||now-anchor.host()>150_000_000L||!access().current(capture)){detail="Native held fallback waiting for current actor/input/pose";return;}
  try{
  for(var slot:capture.slots()){
   if(slot.side()<0||slot.side()>1||anchor.props()[slot.side()]==null||!access().eligible(capture,slot))continue;Drawer drawer=null;
   try{drawer=access().draw(capture,slot);if(drawer==null){detail="Original world phone drawer/resource not ready";continue;}
    if(!enabled||anchor.epoch()!=epoch||!access().current(capture)||!access().drawerCurrent(capture,slot,drawer))throw new IllegalArgumentException("Native drawer provenance changed");
    var correction=NativeHeldMount.correction(capture.base(),anchor.props()[slot.side()],drawer.world());NativeHeldMount.apply(drawer.values(),drawer.flags(),drawer.palette(),drawer.shadow(),drawer.first(),drawer.last(),correction);drawn++;detail="Original CordlessPhone world FBX drawn at native "+(slot.side()==0?"Prop2":"Prop1")+" socket";access().contact(capture,slot,drawer,anchor.epoch());
   }catch(RuntimeException|LinkageError unsupported){if(drawer!=null)access().discard(capture,drawer);rejected++;detail="Native held fallback rejected: "+unsupported.getClass().getSimpleName();}
  }
  NativeHeldContact.consume(frame);
  }finally{NativeHeldContact.finish(frame);}
 }
 static synchronized void adapterForTest(Adapter value){enabled=false;epoch++;anchors.clear();adapter=Objects.requireNonNull(value);}
 static synchronized void configureForTest(boolean value){enabled=value;epoch++;anchors.clear();published=drawn=rejected=0;NativeHeldContact.configure(value);}
 private NativeHeldFallback(){}
}
