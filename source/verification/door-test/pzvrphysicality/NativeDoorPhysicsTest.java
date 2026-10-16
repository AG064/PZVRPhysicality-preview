package pzvrphysicality;
import java.nio.file.Path;import java.util.*;import viewpoint.core.Frame;import viewpointvr.physics.*;import zombie.core.skinnedmodel.model.IsoObjectAnimations;import zombie.core.skinnedmodel.animation.AnimationTrack;
public final class NativeDoorPhysicsTest{
 private static int checks;private static void check(boolean v,String why){if(!v)throw new AssertionError(why);checks++;}
 public static void main(String[] args)throws Exception{
  RigidPhysics.load(Path.of(args[0]));var p=NativeDoorCaptureTest.player();var door=NativeDoorCaptureTest.door(p);var frame=new Frame();var mesh=NativeDoorCaptureTest.mesh();WorldContactCapture.requestItems(true);WorldContactCapture.scope(p);NativeDoorCapture.clear();NativeDoorCaptureTest.offer(door,frame,mesh,DoorMeshCpuTest.identity());var rows=NativeDoorCapture.sample(p);check(rows.size()==1,"SDK-compiled capture produced closed original owner contact");
  float height=(float)Math.sqrt(6)+1;
  try(var world=new PhysicalWorld<Object>()){
   var item=new Object();world.environment(rows);world.add(item,CollisionShape.box(.035f,.035f,.035f,0,0,0),RigidPhysics.pose(10.5f,19.75f,height),.2f);world.hold(item,RigidPhysics.pose(10.5f,20.25f,height),.2f);for(int i=0;i<360;i++)world.step(1f/120);check(world.state(item)[1]<19.99,"captured original closed leaf blocks actual spring body");
   door.open=true;check(NativeDoorCapture.sample(p).isEmpty(),"opening retires stale closed mesh");check(world.environment(NativeDoorCapture.sample(p)),"old native static body retired before new pose");world.wake(item);for(int i=0;i<240;i++)world.step(1f/120);check(world.state(item)[1]>20.15,"retired pose leaves no invisible doorway barrier");
   var track=new AnimationTrack();track.time=.6f;track.finished=true;IsoObjectAnimations.set(door,track);NativeDoorCaptureTest.offer(door,frame,mesh,DoorMeshCpuTest.open());rows=NativeDoorCapture.sample(p);check(rows.size()==1,"current weighted open pose captured");check(world.environment(rows),"actual rotated mesh replaces native environment");world.wake(item);for(int i=0;i<120;i++)world.step(1f/120);check(world.state(item)[1]>20.15,"native open aperture remains clear with leaf collider active");
  }
  try(var world=new PhysicalWorld<Object>()){
   var item=new Object();world.environment(rows);world.add(item,CollisionShape.box(.035f,.035f,.035f,0,0,0),RigidPhysics.pose(9.75f,20.5f,height),.2f);world.hold(item,RigidPhysics.pose(10.25f,20.5f,height),.2f);for(int i=0;i<360;i++)world.step(1f/120);check(world.state(item)[0]<9.99,"actual rotated skinned leaf blocks at native open location");
   door.destroyed=true;check(NativeDoorCapture.sample(p).isEmpty(),"native destruction invalidates skinned leaf");check(world.environment(NativeDoorCapture.sample(p)),"destroyed original body removed");world.wake(item);for(int i=0;i<240;i++)world.step(1f/120);check(world.state(item)[0]>10.15,"destroyed leaf leaves no hidden contact");
  }
  NativeDoorCapture.clear();System.out.println("Native door physics: "+checks+" real Rust checks passed");
 }
}
