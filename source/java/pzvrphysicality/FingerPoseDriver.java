package pzvrphysicality;

import local.pzvr.Engine;
import local.pzvr.XrBackend;
import local.pzvr.VrControls;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.model.SkinningData;
import org.joml.Matrix4f;import org.joml.Quaternionf;import org.joml.Vector3f;
import java.util.*;
import java.lang.ref.WeakReference;

/** Replaces only free-hand finger descendants in the current capture output. */
public final class FingerPoseDriver {
 private record Mode(boolean enabled,long epoch){}
 private static volatile Mode mode=new Mode(false,0);
 private static volatile String detail="Finger poses Off";
 private static final ThreadLocal<ArrayDeque<Scope>> scopes=new ThreadLocal<>();
 private static final ThreadLocal<History[]> histories=new ThreadLocal<>();
 private static final Scope INACTIVE=new Scope();
 static final class Scope {
  final IsoGameCharacter actor;final XrBackend.Frame input;final Thread thread;final long epoch;boolean valid;
  SkinningData skin;Matrix4f[] source,output;
  Scope(){actor=null;input=null;thread=null;epoch=-1;valid=false;}
  Scope(IsoGameCharacter actor,XrBackend.Frame input,long epoch){this.actor=actor;this.input=input;this.epoch=epoch;thread=Thread.currentThread();valid=true;}
 }
 private static final class History {
  WeakReference<IsoGameCharacter> actor;WeakReference<SkinningData> skin;long epoch,generation;int layout;ControllerTouch.Contact contact;
  final FingerPose.AnalogFilter trigger=new FingerPose.AnalogFilter(),squeeze=new FingerPose.AnalogFilter();
 }
 public static void configure(boolean requested){
  if(requested)try{SdkPins.verify(Engine.class,SdkPins.PZVR);SdkPins.verify(IsoPlayer.class,SdkPins.GAME);}catch(Exception|LinkageError unsupported){configureForTest(false);detail="Finger poses unavailable: unsupported SDK owners";return;}
  configureForTest(requested);
 }
 static synchronized void configureForTest(boolean requested){var before=mode;if(before.enabled()!=requested)mode=new Mode(requested,before.epoch()+1);detail=requested?"Finger poses waiting for fresh supported free hands":"Finger poses Off";}
 public static String status(){return mode.enabled()?"Finger poses On: separate index/grip estimates on validated five-finger rigs, native grouped-rig fallback; native grasps take priority":detail;}
 public static boolean enabled(){return mode.enabled();}
 public static void begin(IsoGameCharacter actor){var config=mode;var stack=scopes.get();if(!config.enabled()&&stack==null){histories.remove();return;}if(stack==null){stack=new ArrayDeque<>();scopes.set(stack);}stack.push(config.enabled()?new Scope(actor,Engine.latestInput,config.epoch()):INACTIVE);}
 public static void end(){var stack=scopes.get();if(stack==null)return;if(!stack.isEmpty())stack.pop();if(stack.isEmpty())scopes.remove();}
 public static void frame(SkinningData skin,Matrix4f[] source,Matrix4f[] output,XrBackend.Frame input){
  var s=current();if(s==null||s==INACTIVE)return;if(s.input!=input){s.valid=false;return;}s.skin=skin;s.source=source;s.output=output;
 }
 private static Scope current(){var stack=scopes.get();return stack==null?null:stack.peek();}
 private static boolean free(Scope s,int side){
  var config=mode;if(!config.enabled()||s==null||!s.valid||s.epoch!=config.epoch()||s.thread!=Thread.currentThread()||s.input==null||s.input!=Engine.latestInput||!Engine.active()||!Engine.worldPresent||!s.input.focused()||s.actor==null||s.actor!=IsoPlayer.getInstance()||s.actor.isDead()||VrControls.supportedPistol!=null)return false;
  if((side==0?s.actor.getSecondaryHandItem():s.actor.getPrimaryHandItem())!=null)return false;
  var actions=s.actor.getCharacterActions();if(actions==null)return false;if(!actions.isEmpty()){var action=actions.get(0);if(action==null||action.overrideHandModels)return false;}
  return true;
 }
 public static void pose(SkinningData skin,Matrix4f[] source,Matrix4f[] output,int side,float nativeAmount,Matrix4f[] reference){
  var s=current();if(side<0||side>1)return;if(!free(s,side)){reset(side);return;}
  try{
   if(s.skin!=null&&(s.skin!=skin||s.source!=source||s.output!=output)){s.valid=false;return;}
   if(source==null||output==null||reference==null||source==output||source==reference||output==reference||skin==null||skin.boneIndices==null||skin.skeletonHierarchy==null||source.length!=output.length||source.length!=reference.length||source.length!=skin.skeletonHierarchy.size()||source.length>512)return;
   var reading=ControllerTouch.reading(s.input,side,System.nanoTime());
   String prefix=side==0?"Bip01_L_":"Bip01_R_";Integer hand=skin.boneIndices.get(prefix+"Hand");
   int[] roots=roots(skin,prefix,hand,source.length);if(roots==null)return;
   if(!usable(source[hand])||!usable(output[hand])||!usable(reference[hand]))return;
   int layout=31*(31*source.length+hand)+Arrays.hashCode(roots);layout=31*layout+skin.skeletonHierarchy.hashCode();
   var policy=filtered(s,skin,reading,side,layout);var curls=roots.length==5?new FingerPose.Curl[]{policy.thumb(),policy.index(),policy.grip(),policy.grip(),policy.grip()}:new FingerPose.Curl[]{policy.thumb(),FingerPose.grouped(policy)};
   var updates=new HashMap<Integer,Matrix4f>();
   var baseInverse=new Matrix4f(source[hand]).invert();var refInverse=new Matrix4f(reference[hand]).invert();
   for(int root=0;root<roots.length;root++){
    if(!curls[root].available())continue;
    for(int n=0;n<source.length;n++)if(n==roots[root]||child(skin,n,roots[root])){
     if(n==hand||!usable(source[n])||!usable(reference[n]))return;
     var base=new Matrix4f(baseInverse).mul(source[n]);var target=new Matrix4f(refInverse).mul(reference[n]);
     float amount=curls[root].value();var rotation=base.getUnnormalizedRotation(new Quaternionf()).normalize().slerp(target.getUnnormalizedRotation(new Quaternionf()).normalize(),amount);
     var translation=base.getTranslation(new Vector3f()).lerp(target.getTranslation(new Vector3f()),amount);var scale=base.getScale(new Vector3f()).lerp(target.getScale(new Vector3f()),amount);
     var changed=new Matrix4f(output[hand]).mul(new Matrix4f().translationRotateScale(translation,rotation,scale));if(!changed.isFinite())return;updates.put(n,changed);
    }
   }
   synchronized(FingerPoseDriver.class){if(!free(s,side)||s.input!=Engine.latestInput)return;for(var entry:updates.entrySet())output[entry.getKey()]=entry.getValue();}
  }catch(RuntimeException|LinkageError unsupported){detail="Finger pose unavailable; native pose retained";}
 }
 /** Admit a complete disjoint rig, never a partial or overlapping finger map. */
 private static int[] roots(SkinningData skin,String prefix,Integer hand,int length){
  if(hand==null||!index(hand,length)||!hierarchy(skin))return null;
  int extra=0;for(int n=2;n<5;n++)if(skin.boneIndices.containsKey(prefix+"Finger"+n))extra++;
  if(extra!=0&&extra!=3)return null;
  int[] result=new int[extra==3?5:2];var distinct=new HashSet<Integer>();distinct.add(hand);
  for(int n=0;n<result.length;n++){Integer bone=skin.boneIndices.get(prefix+"Finger"+n);if(bone==null||!index(bone,length)||!distinct.add(bone)||!child(skin,bone,hand))return null;result[n]=bone;}
  for(int a:result)for(int b:result)if(a!=b&&child(skin,a,b))return null;
  return result;
 }
 private static void reset(int side){var values=histories.get();if(values!=null)values[side]=null;}
 private static FingerPose.Pose filtered(Scope s,SkinningData skin,ControllerTouch.Reading reading,int side,int layout){
  if(reading==ControllerTouch.Reading.UNKNOWN||reading.input()==ControllerTouch.Input.UNKNOWN){reset(side);return FingerPose.resolve(null);}
  var values=histories.get();if(values==null){values=new History[2];histories.set(values);}var h=values[side];
  if(h==null||h.actor.get()!=s.actor||h.skin.get()!=skin||h.epoch!=s.epoch||h.generation!=reading.generation()||h.layout!=layout){h=new History();h.actor=new WeakReference<>(s.actor);h.skin=new WeakReference<>(skin);h.epoch=s.epoch;h.generation=reading.generation();h.layout=layout;values[side]=h;}
  var input=reading.input();if(!Objects.equals(h.contact,input.trigger()))h.trigger.reset();h.contact=input.trigger();
  var trigger=h.trigger.sample(input.triggerValue(),reading.hostTime());var squeeze=h.squeeze.sample(input.squeeze(),reading.hostTime());
  return FingerPose.resolve(new ControllerTouch.Input(input.trigger(),input.stick(),input.rest(),input.primary(),input.secondary(),new ControllerTouch.Value(trigger.available(),trigger.value()),new ControllerTouch.Value(squeeze.available(),squeeze.value())));
 }
 private static boolean index(int n,int length){return n>=0&&n<length;}
 private static boolean usable(Matrix4f value){return value!=null&&value.isFinite()&&Math.abs(value.determinant())>1e-8f;}
 private static boolean hierarchy(SkinningData skin){int count=skin.skeletonHierarchy.size();for(int n=0;n<count;n++){int at=n;boolean root=false;for(int hops=0;hops<count;hops++){if(!index(at,count))return false;Integer parent=skin.skeletonHierarchy.get(at);if(parent==null)return false;if(parent==-1){root=true;break;}at=parent;}if(!root)return false;}return true;}
 private static boolean child(SkinningData skin,int bone,int ancestor){int at=bone;for(int hops=0;hops<skin.skeletonHierarchy.size();hops++){if(!index(at,skin.skeletonHierarchy.size()))return false;Integer parent=skin.skeletonHierarchy.get(at);if(parent==null||parent<0)return false;if(parent==ancestor)return true;if(parent==at)return false;at=parent;}return false;}
 private FingerPoseDriver(){}
}
