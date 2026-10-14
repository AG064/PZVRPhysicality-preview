package pzvrphysicality;
import java.util.*;import local.pzvr.*;
/** Detached native gesture ownership. Open outside authored monitors; raw input is never changed. */
public final class NativeInputOwnership {
 private static final long MAX_AGE=250_000_000L;
 public record Snapshot(XrBackend.Frame input,long time,boolean available,boolean bandaging,boolean leftBlocked,boolean rightBlocked,int bindingHand,long leftGeneration,long rightGeneration){
  public boolean current(){long now=System.nanoTime();return available&&input!=null&&input==Engine.latestInput&&input.focused()&&now>=time&&now-time<MAX_AGE&&input.time()>0&&now>=input.time()&&now-input.time()<MAX_AGE;}
  public boolean globalBlocked(){return !current()||bandaging||global;}
  public long generation(int side){return side==0?leftGeneration:side==1?rightGeneration:-1;}
  public boolean allows(int side){return side>=0&&side<2&&!globalBlocked()&&generation(side)==(side==0?leftEpoch:rightEpoch)&&!(side==0?leftBlocked:rightBlocked);}
 }
 public static final class Scope implements AutoCloseable {
  private final Thread owner=Thread.currentThread();private final Scope previous;private final Snapshot snapshot;private boolean closed;
  private Scope(Scope before,Snapshot value){previous=before;snapshot=value;}
  public Snapshot snapshot(){return snapshot;}
  public void close(){if(closed)return;if(Thread.currentThread()!=owner||scoped.get()!=this)throw new IllegalStateException("Native ownership scope order/thread");closed=true;if(previous==null)scoped.remove();else scoped.set(previous);}
 }
 private static final ThreadLocal<Scope> scoped=new ThreadLocal<>();private static final boolean[] neutral={false,false},priorClaim={false,false};private static final XrBackend.Frame[] claimFrame=new XrBackend.Frame[2];private static final long[] claimTime={Long.MIN_VALUE,Long.MIN_VALUE};private static long sequence=1;private static volatile long leftEpoch=1,rightEpoch=1;private static volatile boolean global;
 public static Scope open(){var before=scoped.get();var value=before==null?capture():before.snapshot;var result=new Scope(before,value);scoped.set(result);return result;}
 public static Snapshot read(){var scope=scoped.get();return scope==null?capture():scope.snapshot;}
 public static boolean allows(int side){return read().allows(side);}public static boolean globalBlocked(){return read().globalBlocked();}
 public static long generation(int side){return read().generation(side);}
 /** Passive native update receipt catches claims while authored requests remain queued. */
 public static void observe(Throwable failure){if(failure==null){capture();return;}synchronized(NativeInputOwnership.class){global=true;for(int side=0;side<2;side++){neutral[side]=true;priorClaim[side]=true;claimFrame[side]=Engine.latestInput;claimTime[side]=claimFrame[side]==null?Long.MIN_VALUE:claimFrame[side].time();if(side==0)leftEpoch=++sequence;else rightEpoch=++sequence;}}}
 /** Logical binding slots are mirrored independently of aim/UI dominant-hand preference. */
 public static int bindingHandIndex(){var scope=scoped.get();return scope!=null?scope.snapshot.bindingHand():binding();}
 private static int binding(){try{int side=Bindings.handIndex(Engine.settings,true);return side==0||side==1?side:-1;}catch(RuntimeException|LinkageError unavailable){return -1;}}
 private static Snapshot capture(){var input=Engine.latestInput;long now=System.nanoTime();boolean available=true,bandaging=true;boolean[] claimed={true,true};int binding=-1;
  // These native providers can lock internally. Never call them under our monitor or an authored route monitor.
  try{bandaging=PhysicalBandaging.blocksCombat();for(int side=0;side<2;side++)claimed[side]=VrApi.isHandGrabbed(side)||VrApi.gripConsumed(side);binding=binding();available=input!=null&&input==Engine.latestInput&&input.focused()&&input.time()>0&&now>=input.time()&&now-input.time()<MAX_AGE;}catch(RuntimeException|LinkageError unavailable){available=false;}
  boolean[] blocked=new boolean[2];long left,right;synchronized(NativeInputOwnership.class){global=!available||bandaging;for(int side=0;side<2;side++){
   var hand=input==null?null:side==0?input.left():input.right();boolean owns=!available||bandaging||claimed[side]||hand==null||!hand.tracked()||hand.grip()==null;if(owns){if(!priorClaim[side]){if(side==0)leftEpoch=++sequence;else rightEpoch=++sequence;}neutral[side]=true;claimFrame[side]=input;claimTime[side]=input==null?Long.MIN_VALUE:input.time();}priorClaim[side]=owns;
   boolean released=hand!=null&&input.focused()&&hand.tracked()&&hand.grip()!=null&&!hand.down(XrBackend.GRIP)&&!hand.down(XrBackend.TRIGGER);
   if(!owns&&neutral[side]&&released&&input!=claimFrame[side]&&input.time()>claimTime[side])neutral[side]=false;
   blocked[side]=owns||neutral[side];
  }left=leftEpoch;right=rightEpoch;}return new Snapshot(input,now,available,bandaging,blocked[0],blocked[1],binding,left,right);
 }
 static synchronized void resetForTest(){Arrays.fill(neutral,false);Arrays.fill(priorClaim,false);Arrays.fill(claimFrame,null);Arrays.fill(claimTime,Long.MIN_VALUE);leftEpoch=rightEpoch=sequence=1;global=false;scoped.remove();}
 private NativeInputOwnership(){}
}
