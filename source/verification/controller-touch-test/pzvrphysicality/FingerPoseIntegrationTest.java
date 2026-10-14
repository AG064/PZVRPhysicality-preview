package pzvrphysicality;
import java.util.*;import local.pzvr.Engine;import local.pzvr.XrBackend;import local.pzvr.VrControls;import zombie.characters.IsoPlayer;import zombie.inventory.InventoryItem;import zombie.core.skinnedmodel.model.SkinningData;import org.joml.Matrix4f;
/** Advice callbacks and production layer with authored player and bone records. */
public final class FingerPoseIntegrationTest {
 private static int checks;private static void check(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
 private static SkinningData skin;private static Matrix4f[] source,output,reference;private static IsoPlayer actor;private static ControllerTouchFixture fixture;private static ControllerTouch.Context context;private static XrBackend.Frame frame;
 private static Matrix4f[] matrices(){var result=new Matrix4f[9];for(int n=0;n<result.length;n++)result[n]=new Matrix4f().translation(n*.1f,n*.2f,n*.3f);return result;}
 private static float[][] values(Matrix4f[] array){float[][] result=new float[array.length][];for(int n=0;n<array.length;n++)result[n]=array[n].get(new float[16]);return result;}
 private static boolean same(Matrix4f[] array,float[][] before){for(int n=0;n<array.length;n++)if(!Arrays.equals(array[n].get(new float[16]),before[n]))return false;return true;}
 private static void arrays(){source=matrices();output=matrices();reference=matrices();output[1].translate(2,3,4);output[5].translate(-2,3,4);for(int n:new int[]{2,3,4,6,7})reference[n].rotateZ(.7f);}
 private static void setup(){
  NativeInputOwnership.resetForTest();local.pzvr.PhysicalBandaging.blocked=local.pzvr.PhysicalBandaging.fail=false;java.util.Arrays.fill(local.pzvr.VrApi.grabbed,false);java.util.Arrays.fill(local.pzvr.VrApi.consumed,false);Engine.settings=new local.pzvr.Settings();
  fixture=new ControllerTouchFixture();ControllerTouch.transportForTest(fixture);Object lock=new Object();context=ControllerTouchFixture.context(new Object(),new Object(),new Object(),lock,true);check(ControllerTouch.suggest(context,ControllerTouchFixture.profile(),null),"authored passive registration");ControllerTouch.actionsCreated(context,true);fixture.supported(0);fixture.supported(1);fixture.contact(ControllerTouch.Channel.STICK_TOUCH,0,true);fixture.value(ControllerTouch.Channel.SQUEEZE_VALUE,0,.8f);fixture.value(ControllerTouch.Channel.TRIGGER_VALUE,0,.2f);
  var left=ControllerTouchFixture.hand(true);var right=ControllerTouchFixture.hand(true);synchronized(lock){ControllerTouch.capture(context,null,900,new XrBackend.Hand[]{left,right},System.nanoTime());}frame=ControllerTouchFixture.frame(System.nanoTime(),left,right);Engine.latestInput=frame;Engine.latest=ControllerTouchFixture.frame(System.nanoTime(),ControllerTouchFixture.hand(true),ControllerTouchFixture.hand(true));Engine.enabled=Engine.worldPresent=true;
  actor=new IsoPlayer();IsoPlayer.instance=actor;VrControls.supportedPistol=null;skin=new SkinningData();String[] names={"Bip01_Arm","Bip01_L_Hand","Bip01_L_Finger0","Bip01_L_Finger1","Bip01_L_Finger11","Bip01_R_Hand","Bip01_R_Finger0","Bip01_R_Finger1","Bip01_Prop1"};for(int n=0;n<names.length;n++)skin.boneIndices.put(names[n],n);skin.skeletonHierarchy.addAll(List.of(-1,0,1,1,3,0,5,5,1));arrays();FingerPoseDriver.configureForTest(true);
  synchronized(lock){ControllerTouch.capture(context,null,900,new XrBackend.Hand[]{left,right},System.nanoTime());}frame=ControllerTouchFixture.frame(System.nanoTime(),left,right);Engine.latestInput=frame;
 }
 private static void enter(){FingerCaptureAdvice.enter(actor);FingerFrameAdvice.enter(skin,source,output,frame);}
 private static void pose(int side){FingerPoseAdvice.exit(skin,source,output,side,.5f,reference);}
 private static void unchanged(String why,Runnable change){arrays();change.run();var before=values(output);enter();try{pose(0);}finally{FingerCaptureAdvice.exit();}check(same(output,before),why);}
 private static void publish(float squeeze,float trigger,boolean touched,long sampleTime){fixture.supported(0);fixture.value(ControllerTouch.Channel.SQUEEZE_VALUE,0,squeeze);fixture.value(ControllerTouch.Channel.TRIGGER_VALUE,0,trigger);fixture.contact(ControllerTouch.Channel.TRIGGER_TOUCH,0,touched);synchronized(context.lock){ControllerTouch.capture(context,null,900,new XrBackend.Hand[]{frame.left(),frame.right()},sampleTime);}Engine.latestInput=frame=ControllerTouchFixture.frame(System.nanoTime(),frame.left(),frame.right());}
 private static float groupCurl(){return new Matrix4f(output[1]).invert().mul(output[3]).getUnnormalizedRotation(new org.joml.Quaternionf()).normalize().angle()/.7f;}
 private static float thumbCurl(){return new Matrix4f(output[1]).invert().mul(output[2]).getUnnormalizedRotation(new org.joml.Quaternionf()).normalize().angle()/.7f;}
 private static void freePose(){arrays();enter();try{pose(0);}finally{FingerCaptureAdvice.exit();}}
 private static void nativeOwnership(){
  setup();Engine.settings.dominantHand="left";Engine.settings.mirrorControllerBindings=true;local.pzvr.VrApi.grabbed[0]=true;arrays();var before=values(output);enter();try{pose(0);pose(1);}finally{FingerCaptureAdvice.exit();}boolean unchanged=true;for(int n:new int[]{1,2,3,4})unchanged&=Arrays.equals(output[n].get(new float[16]),before[n]);check(unchanged,"empty inventory left native grab preserves exact native wrist/fingers");check(!Arrays.equals(output[6].get(new float[16]),before[6]),"unclaimed physical right still receives finger layer under left aim and mirrored binding");check(ControllerTouch.input(frame,0,System.nanoTime()).squeeze().value()==.8f,"native grab does not erase separate raw pressure telemetry");
  local.pzvr.VrApi.grabbed[0]=false;arrays();before=values(output);enter();try{pose(0);}finally{FingerCaptureAdvice.exit();}check(same(output,before),"native grab release cannot reuse same capture permission");publish(.8f,.2f,false,System.nanoTime()-1000);freePose();check(Math.abs(groupCurl()-.8f)<.0001f,"fresh neutral physical frame restores supported free-hand pressure pose");
  local.pzvr.PhysicalBandaging.blocked=true;unchanged("native wrapping/pending bandage keeps native finger pose",()->{});local.pzvr.PhysicalBandaging.blocked=false;publish(.7f,.2f,false,System.nanoTime()-1000);freePose();check(Math.abs(groupCurl()-.7f)<.0001f,"bandage release resumes only after fresh neutral frame");
  local.pzvr.VrApi.consumed[0]=true;unchanged("consumed native grip preserves original free-hand output",()->{});local.pzvr.VrApi.consumed[0]=false;publish(.7f,.2f,false,System.nanoTime()-1000);freePose();actor.left=new InventoryItem();unchanged("native held item retains grasp after latest ownership integration",()->{});actor.left=null;
 }
 private static void fiveArrays(){
  source=new Matrix4f[16];output=new Matrix4f[16];reference=new Matrix4f[16];
  for(int n=0;n<16;n++){source[n]=new Matrix4f().translation(n*.1f,n*.2f,n*.3f);output[n]=new Matrix4f(source[n]);reference[n]=new Matrix4f(source[n]);}
  output[1].translate(2,3,4);output[5].translate(-2,3,4);
  for(int n:new int[]{2,3,4,6,7,9,10,11,12,13,14})reference[n].rotateZ(.7f);
 }
 private static float rootCurl(int hand,int root){return new Matrix4f(output[hand]).invert().mul(output[root]).getUnnormalizedRotation(new org.joml.Quaternionf()).normalize().angle()/.7f;}
 private static void capturedPose(int side){enter();try{pose(side);}finally{FingerCaptureAdvice.exit();}}
 private static void tombGroupedRig(){
  // Authored 32-bone layout with the privately verified Tomb hand/finger parent relationships. No asset coordinates.
  setup();skin=new SkinningData();String[] names={"Bip01_Pelvis","Bip01_Spine","Bip01_Spine1","Bip01_Neck","Bip01_Head","Bip01_L_Clavicle","Bip01_L_UpperArm","Bip01_L_Forearm","Bip01_L_Hand","Bip01_L_Finger0","Bip01_L_Finger1","Bip01_R_Clavicle","Bip01_R_UpperArm","Bip01_R_Forearm","Bip01_R_Hand","Bip01_R_Finger0","Bip01_R_Finger1"};
  int[] parents={-1,0,1,2,3,3,5,6,7,8,8,3,11,12,13,14,14};
  source=new Matrix4f[32];output=new Matrix4f[32];reference=new Matrix4f[32];
  for(int n=0;n<32;n++){skin.boneIndices.put(n<names.length?names[n]:"AuthoredRemainingBone"+n,n);skin.skeletonHierarchy.add(n<parents.length?parents[n]:0);source[n]=new Matrix4f().translation(n*.02f,n*.03f,n*.04f);output[n]=new Matrix4f(source[n]);reference[n]=new Matrix4f(source[n]);}
  for(int n:new int[]{9,10,15,16})reference[n].rotateZ(.7f);
  var before=values(output);var original=values(source);var target=values(reference);capturedPose(0);
  check(Math.abs(rootCurl(8,9)-.65f)<.0001f,"actual Tomb grouped-root thumb relationship supports capacitive thumb pose");
  check(Math.abs(rootCurl(8,10)-.8f)<.0001f,"actual Tomb grouped non-thumb root supports grip/trigger estimate");
  check(Arrays.equals(output[8].get(new float[16]),before[8])&&Arrays.equals(output[14].get(new float[16]),before[14]),"Tomb grouped finger layer preserves both authoritative wrists");
  check(same(source,original)&&same(reference,target),"Tomb-compatible source and reference data remain immutable");
  fixture.value(ControllerTouch.Channel.SQUEEZE_VALUE,1,.25f);fixture.value(ControllerTouch.Channel.TRIGGER_VALUE,1,.9f);fixture.contact(ControllerTouch.Channel.STICK_TOUCH,1,true);
  synchronized(context.lock){ControllerTouch.capture(context,null,900,new XrBackend.Hand[]{frame.left(),frame.right()},System.nanoTime()-1000);}Engine.latestInput=frame=ControllerTouchFixture.frame(System.nanoTime(),frame.left(),frame.right());capturedPose(1);
  check(Math.abs(rootCurl(14,15)-.65f)<.0001f&&Math.abs(rootCurl(14,16)-.9f)<.0001f,"right Tomb grouped rig responds to right thumb contact and greater trigger pressure independently");
 }
 private static void fiveFingerRigs(){
  setup();skin.skeletonHierarchy.addAll(List.of(1,1,1,9,10,11,1));
  for(int n=2;n<5;n++)skin.boneIndices.put("Bip01_L_Finger"+n,n+7);
  publish(.8f,.2f,false,System.nanoTime()-1000);fiveArrays();var before=values(output);var original=values(source);var target=values(reference);capturedPose(0);
  check(Math.abs(rootCurl(1,3)-.2f)<.0001f,"five-root index follows independent trigger pressure");
  for(int root:new int[]{9,10,11})check(Math.abs(rootCurl(1,root)-.8f)<.0001f,"five-root grip finger follows squeeze independently "+root);
  for(int n:new int[]{4,12,13,14})check(!Arrays.equals(output[n].get(new float[16]),before[n]),"five-root descendant follows its own root "+n);
  for(int n:new int[]{0,1,5,6,7,8,15})check(Arrays.equals(output[n].get(new float[16]),before[n]),"five-root wrist/other hand/socket unchanged "+n);
  check(same(source,original)&&same(reference,target),"five-root source and reference remain immutable");
  publish(.1f,.9f,false,System.nanoTime()-1000);fiveArrays();capturedPose(0);check(Math.abs(rootCurl(1,3)-.9f)<.0001f&&Math.abs(rootCurl(1,9)-.1f)<.0001f,"trigger never closes separate grip fingers");
  fiveArrays();before=values(output);skin.boneIndices.remove("Bip01_L_Finger4");capturedPose(0);check(same(output,before),"partial independent rig retains entire native pose");skin.boneIndices.put("Bip01_L_Finger4",11);
  fiveArrays();before=values(output);skin.boneIndices.put("Bip01_L_Finger3",9);capturedPose(0);check(same(output,before),"duplicate finger root rejected atomically");skin.boneIndices.put("Bip01_L_Finger3",10);
  fiveArrays();before=values(output);skin.skeletonHierarchy.set(10,9);capturedPose(0);check(same(output,before),"overlapping independent finger roots rejected");skin.skeletonHierarchy.set(10,1);
  fiveArrays();before=values(output);reference[14].m00(Float.NaN);capturedPose(0);check(same(output,before),"invalid little-finger descendant preserves complete native output");
  fiveArrays();before=values(output);actor.left=new InventoryItem();capturedPose(0);check(same(output,before),"five-root held grasp remains native");actor.left=null;
  for(int n=2;n<5;n++){skin.boneIndices.remove("Bip01_L_Finger"+n);skin.boneIndices.put("Bip01_R_Finger"+n,n+7);skin.skeletonHierarchy.set(n+7,5);}
  fixture.value(ControllerTouch.Channel.SQUEEZE_VALUE,1,.35f);fixture.value(ControllerTouch.Channel.TRIGGER_VALUE,1,.85f);fixture.contact(ControllerTouch.Channel.TRIGGER_TOUCH,1,false);
  synchronized(context.lock){ControllerTouch.capture(context,null,900,new XrBackend.Hand[]{frame.left(),frame.right()},System.nanoTime()-1000);}Engine.latestInput=frame=ControllerTouchFixture.frame(System.nanoTime(),frame.left(),frame.right());
  fiveArrays();before=values(output);capturedPose(1);check(Math.abs(rootCurl(5,7)-.85f)<.0001f&&Math.abs(rootCurl(5,9)-.35f)<.0001f,"right independent rig reads right sensors");check(Arrays.equals(output[3].get(new float[16]),before[3]),"right independent rig preserves opposite hand");
 }
 public static void main(String[] args)throws Exception {
  setup();var before=values(output);var original=values(source);var target=values(reference);enter();try{pose(0);}finally{FingerCaptureAdvice.exit();}
  check(!Arrays.equals(output[2].get(new float[16]),before[2]),"thumb uses independent supported contact");check(!Arrays.equals(output[3].get(new float[16]),before[3]),"group uses squeeze estimate");check(!Arrays.equals(output[4].get(new float[16]),before[4]),"recognized finger descendants updated");
  for(int n:new int[]{0,1,5,6,7,8})check(Arrays.equals(output[n].get(new float[16]),before[n]),"arm/wrist/other hand/item socket remains authoritative "+n);
  check(same(source,original)&&same(reference,target),"shared source and grasp-reference matrices unchanged");check(output[2]!=source[2]&&output[3]!=reference[3],"changed matrices owned by capture output");
  check(FingerPoseDriver.status().contains("grouped applied"),"grouped native hand status identifies real applied finger matrices");
  arrays();before=values(output);pose(0);check(same(output,before),"finger callback outside capture scope ignored");
  unchanged("Off keeps native pose",()->FingerPoseDriver.configureForTest(false));FingerPoseDriver.configureForTest(true);
  unchanged("held left slot keeps native grasp",()->actor.left=new InventoryItem());actor.left=null;
  arrays();actor.right=new InventoryItem();before=values(output);enter();try{pose(1);pose(0);}finally{FingerCaptureAdvice.exit();}check(Arrays.equals(output[6].get(new float[16]),before[6])&&Arrays.equals(output[7].get(new float[16]),before[7]),"held right slot keeps native grasp");check(!Arrays.equals(output[2].get(new float[16]),before[2]),"empty opposite hand remains independent");actor.right=null;
  unchanged("active pistol support keeps both native hands",()->VrControls.supportedPistol=new InventoryItem());VrControls.supportedPistol=null;
  var action=new zombie.characters.CharacterTimedActions.BaseAction();action.overrideHandModels=true;unchanged("timed action hand override retains native pose",()->actor.actions.add(action));actor.actions.clear();
  unchanged("dead actor retains native pose",()->actor.dead=true);actor.dead=false;unchanged("other actor capture cannot use local input",()->IsoPlayer.instance=new IsoPlayer());IsoPlayer.instance=actor;
  unchanged("inactive engine fallback",()->Engine.enabled=false);Engine.enabled=true;unchanged("missing world fallback",()->Engine.worldPresent=false);Engine.worldPresent=true;
  unchanged("changed latestInput rejected",()->Engine.latestInput=Engine.latest);Engine.latestInput=frame;
  arrays();before=values(output);enter();FingerFrameAdvice.enter(skin,source,output,Engine.latest);pose(0);FingerCaptureAdvice.exit();check(same(output,before),"posture binds exact captured input frame");
  arrays();before=values(output);enter();FingerCaptureAdvice.enter(new IsoPlayer());pose(0);check(same(output,before),"nested foreign capture does not borrow outer scope");FingerCaptureAdvice.exit();pose(0);FingerCaptureAdvice.exit();check(!same(output,before),"nested exit restores outer capture scope");
  arrays();before=values(output);enter();try{throw new IllegalStateException("authored original capture failure");}catch(IllegalStateException expected){}finally{FingerCaptureAdvice.exit();}pose(0);check(same(output,before),"failed capture exit restores empty scope");
  arrays();before=values(output);enter();Thread worker=new Thread(()->pose(0));worker.start();worker.join();FingerCaptureAdvice.exit();check(same(output,before),"other thread cannot mutate owned capture");
  arrays();before=values(output);FingerCaptureAdvice.enter(actor);try{pose(0);}finally{FingerCaptureAdvice.exit();}check(!same(output,before),"capture still supports native physical-crouch Off path without posture");
  unchanged("unknown rig root falls back",()->skin.boneIndices.remove("Bip01_L_Finger0"));skin.boneIndices.put("Bip01_L_Finger0",2);
  unchanged("cyclic hierarchy falls back",()->skin.skeletonHierarchy.set(4,4));skin.skeletonHierarchy.set(4,3);
  arrays();output=source;before=values(source);enter();try{pose(0);}finally{FingerCaptureAdvice.exit();}check(same(source,before),"shared output/source array rejected");
  check(FingerPoseDriver.status().contains("source/reference/layout unavailable"),"native reference fallback is explicit rather than an unconditional On label");
  arrays();reference[3].m00(Float.NaN);before=values(output);enter();try{pose(0);}finally{FingerCaptureAdvice.exit();}check(same(output,before),"invalid group reference causes atomic native fallback");
  arrays();source[1].zero();before=values(output);enter();try{pose(0);}finally{FingerCaptureAdvice.exit();}check(same(output,before),"collapsed hand transform rejected");
  FingerPoseDriver.configureForTest(false);var field=FingerPoseDriver.class.getDeclaredField("scopes");field.setAccessible(true);var scopes=(ThreadLocal<?>)field.get(null);FingerCaptureAdvice.enter(actor);check(scopes.get()==null,"Off capture does not allocate a scope/deque");FingerCaptureAdvice.exit();FingerPoseDriver.configureForTest(true);
  long sampled=System.nanoTime()-30_000_000L;publish(.48f,0,false,sampled);freePose();check(Math.abs(groupCurl()-.48f)<.0001f,"fresh provenance starts at actual pressure");
  publish(.52f,0,false,sampled+8_333_333L);freePose();float smooth=groupCurl();check(smooth>.48f&&smooth<.52f,"avatar layer filters small analog change across fresh output arrays");check(ControllerTouch.input(frame,0,System.nanoTime()).squeeze().value()==.52f,"avatar filter never alters raw controller input");
  freePose();check(Math.abs(groupCurl()-smooth)<.0001f,"same sample yields same pose across repeated captures");publish(1,0,false,sampled+16_666_666L);freePose();check(Math.abs(groupCurl()-1)<.0001f,"large analog grip transition reaches visible closed pose immediately");
  publish(0,0,true,System.nanoTime()-1000);freePose();check(Math.abs(groupCurl()-.18f)<.0001f,"capacitive trigger transition remains immediate");fixture.contact(ControllerTouch.Channel.STICK_TOUCH,0,true);synchronized(context.lock){ControllerTouch.capture(context,null,900,new XrBackend.Hand[]{frame.left(),frame.right()},System.nanoTime()-1000);}Engine.latestInput=frame=ControllerTouchFixture.frame(System.nanoTime(),frame.left(),frame.right());freePose();check(Math.abs(thumbCurl()-.65f)<.0001f,"capacitive thumb transition bypasses analog filter");
  arrays();before=values(output);enter();FingerPoseDriver.configureForTest(false);FingerCaptureAdvice.enter(actor);FingerFrameAdvice.enter(skin,source,output,frame);pose(0);FingerCaptureAdvice.exit();check(same(output,before),"Off nested scope cannot mutate its outer capture");FingerPoseDriver.configureForTest(true);pose(0);FingerCaptureAdvice.exit();check(same(output,before),"Off-to-On invalidates old capture epoch");publish(.5f,0,false,System.nanoTime()-1000);freePose();check(Math.abs(groupCurl()-.5f)<.0001f,"new epoch starts immediately without old curl history");
  fixture.rightProfile=999;publish(.54f,0,false,System.nanoTime()-1000);freePose();check(Math.abs(groupCurl()-.54f)<.0001f,"profile generation change resets avatar filter history");fixture.rightProfile=101;
  publish(.54f,0,false,System.nanoTime()-1000);arrays();source[1].rotateZ(.4f).scale(1.1f);source[3]=new Matrix4f(source[1]).translate(.2f,.3f,.4f);reference[1].rotateZ(-.2f).scale(.9f);reference[3]=new Matrix4f(reference[1]).translate(.2f,.3f,.4f).rotateZ(.7f);output[1].rotateX(.25f);original=values(source);target=values(reference);var wrist=new Matrix4f(output[1]);enter();try{pose(0);}finally{FingerCaptureAdvice.exit();}check(Math.abs(groupCurl()-.54f)<.0001f,"pose uses hand-relative source/reference under different animation transforms");check(output[1].equals(wrist)&&same(source,original)&&same(reference,target),"animated wrist/source/reference remain untouched");
  actor=new IsoPlayer();IsoPlayer.instance=actor;publish(.5f,0,false,System.nanoTime()-1000);freePose();check(Math.abs(groupCurl()-.5f)<.0001f,"new current actor cannot inherit old analog smoothing");
  var nextSkin=new SkinningData();nextSkin.boneIndices.putAll(skin.boneIndices);nextSkin.skeletonHierarchy.addAll(skin.skeletonHierarchy);skin=nextSkin;publish(.52f,0,false,System.nanoTime()-1000);freePose();check(Math.abs(groupCurl()-.52f)<.0001f,"new rig identity resets smoothing");
  skin.skeletonHierarchy.set(4,2);publish(.56f,0,false,System.nanoTime()-1000);freePose();check(Math.abs(groupCurl()-.56f)<.0001f,"reference bone layout change resets smoothing");skin.skeletonHierarchy.set(4,3);
  ControllerTouch.close(context.backend);context=ControllerTouchFixture.context(context.backend,new Object(),new Object(),context.lock,true);check(ControllerTouch.suggest(context,ControllerTouchFixture.profile(),null),"authored replacement session registers its own passive actions");ControllerTouch.actionsCreated(context,true);publish(.6f,0,false,System.nanoTime()-1000);freePose();check(Math.abs(groupCurl()-.6f)<.0001f,"new session generation resets smoothing");
  arrays();fixture.touch.clear();fixture.analog.clear();synchronized(context.lock){ControllerTouch.capture(context,null,900,new XrBackend.Hand[]{frame.left(),frame.right()},System.nanoTime()-1000);}Engine.latestInput=frame=ControllerTouchFixture.frame(System.nanoTime(),frame.left(),frame.right());before=values(output);enter();try{pose(0);}finally{FingerCaptureAdvice.exit();}check(same(output,before),"unavailable raw sensors preserve native pose");
  tombGroupedRig();fiveFingerRigs();nativeOwnership();FingerPoseDriver.configure(true);check(FingerPoseDriver.status().contains("unsupported SDK owners"),"runtime enable rejects authored SDK identities");FingerPoseDriver.configureForTest(false);
  check(FingerPoseDriver.status().contains("Off"),"Off state explicit");
  System.out.println("Finger pose integration: "+checks+" checks passed; authored advice callbacks and bones only");
 }
}
