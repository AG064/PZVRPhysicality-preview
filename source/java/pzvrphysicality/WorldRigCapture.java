package pzvrphysicality;

import java.util.ArrayList;
import local.pzvr.*;

/** Readonly rig publication. Uses upstream rigOffset rather than duplicating camera/recenter/world-scale math. */
public final class WorldRigCapture {
 public static volatile WorldRigFrame latest;
 private static volatile boolean requested;
 private static boolean tapsRequested,storageRequested,utensilsRequested,bodyRequested,curtainsRequested,backpackRequested,heldContactsRequested;
 public static volatile BodyRig body;
 private static final java.util.Map<Object,WorldRigFrame> frames=new java.util.IdentityHashMap<>();
 public static synchronized void request(boolean value){tapsRequested=value;requests();}
 public static synchronized void requestStorage(boolean value){storageRequested=value;requests();}
 public static synchronized void requestUtensils(boolean value){utensilsRequested=value;requests();}
 public static synchronized void requestBody(boolean value){bodyRequested=value;if(!value)body=null;requests();}
 public static synchronized void requestCurtains(boolean value){curtainsRequested=value;requests();}
 public static synchronized void requestBackpack(boolean value){backpackRequested=value;requests();}
 public static synchronized void requestHeldContacts(boolean value){heldContactsRequested=value;requests();}
 private static void requests(){requested=tapsRequested||storageRequested||utensilsRequested||bodyRequested||curtainsRequested||backpackRequested||heldContactsRequested;if(!requested){latest=null;body=null;frames.clear();}}
 public static synchronized WorldRigFrame forScene(Object scene,long now){var frame=frames.get(scene);return requested&&latest!=null&&frame!=null&&frame.fresh(now)?frame:null;}
 private static Object actor,origin;private static double yaw=Double.NaN;private static long generation;
 private static double units=Double.NaN,mouthDown=Double.NaN;
 private static Class<?> frameType;private static java.lang.reflect.Field camX,camY,camZ,scene;
 private static ContactFrame.Point subtract(ContactFrame.Point a,ContactFrame.Point b){return new ContactFrame.Point(a.x()-b.x(),a.y()-b.y(),a.height()-b.height());}
 private static ContactFrame.Point world(Object frame,Math3.Vec physical)throws ReflectiveOperationException{
  var offset=Engine.rigOffset(physical);var type=frame.getClass();if(frameType!=type){camX=type.getField("camX");camY=type.getField("camY");camZ=type.getField("camZ");scene=type.getField("scene");frameType=type;}return WorldRigFrame.fromRender(camX.getFloat(frame),camY.getFloat(frame),camZ.getFloat(frame),new MouthMath.Vec(offset.x(),offset.y(),offset.z()));
 }
 public static void capture(Object frame,Object player,XrBackend.Frame input){
  if(!requested||!Main.ready()||!Engine.active()||!Engine.worldPresent||player==null||input==null||input!=Engine.latestInput||!input.focused()||input.head()==null){latest=null;return;}
  try{
   Object centre=Engine.trackingOrigin();double facing=Engine.playspaceYaw,down=NativeConsumption.mouthDown();
   if(player!=actor||!java.util.Objects.equals(centre,origin)||!Double.isFinite(yaw)||Math.abs(yaw-facing)>.001){actor=player;origin=centre;yaw=facing;generation++;}
   var head=input.head();var mouth=world(frame,head.position().add(head.rotation().rotate(new Math3.Vec(0,-down,-.08))));
   var hands=new ArrayList<WorldRigFrame.Hand>();
   for(int side=0;side<2;side++){
    var hand=side==0?input.left():input.right();if(hand==null||!hand.tracked()||hand.grip()==null)continue;
    var pose=hand.grip();var grip=world(frame,pose.position());
    var x=subtract(world(frame,pose.position().add(pose.rotation().rotate(new Math3.Vec(1,0,0)))),grip);
    var y=subtract(world(frame,pose.position().add(pose.rotation().rotate(new Math3.Vec(0,1,0)))),grip);
    var z=subtract(world(frame,pose.position().add(pose.rotation().rotate(new Math3.Vec(0,0,1)))),grip);
    hands.add(new WorldRigFrame.Hand(side,grip,x,y,z,hand.down(XrBackend.TRIGGER)));
   }
   var camera=new ContactFrame.Point(camX.getFloat(frame),camY.getFloat(frame),camZ.getFloat(frame)*Math.sqrt(6));
   if(!requested||input!=Engine.latestInput||!java.util.Objects.equals(centre,Engine.trackingOrigin())||Math.abs(facing-Engine.playspaceYaw)>.001||down!=NativeConsumption.mouthDown()||hands.isEmpty()){latest=null;return;}
   double scale=hands.getFirst().units();
   if(!Double.isFinite(units)||Math.abs(units-scale)>.0001||mouthDown!=down){units=scale;mouthDown=down;generation++;}
   var publication=new WorldRigFrame(System.nanoTime(),input.time(),player,scene.get(frame),camera,generation,mouth,hands);
   synchronized(WorldRigCapture.class){
    if(!requested||input!=Engine.latestInput){latest=null;return;}
    if(frames.size()>=6&&!frames.containsKey(publication.scene())){var oldest=frames.entrySet().stream().min(java.util.Comparator.comparingLong(e->e.getValue().hostTime())).orElseThrow();frames.remove(oldest.getKey());}
    frames.put(publication.scene(),publication);latest=publication;
    if(bodyRequested){var centreHead=world(frame,head.position());var headRight=subtract(world(frame,head.position().add(head.rotation().rotate(new Math3.Vec(1,0,0)))),centreHead);var headForward=subtract(world(frame,head.position().add(head.rotation().rotate(new Math3.Vec(0,0,-1)))),centreHead);body=input==Engine.latestInput?BodyRig.from(publication,centreHead,headRight,headForward,scale):null;}
   }
  }catch(ReflectiveOperationException|RuntimeException unsupported){latest=null;}
 }
 private WorldRigCapture(){}
}
