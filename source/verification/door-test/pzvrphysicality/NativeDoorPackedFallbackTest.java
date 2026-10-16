package pzvrphysicality;
import java.util.*;import viewpoint.core.Frame;import viewpointvr.physics.PackedModelGeometry;import zombie.core.skinnedmodel.model.*;import zombie.core.skinnedmodel.animation.AnimationTrack;
public final class NativeDoorPackedFallbackTest{
 private static int checks;private static void check(boolean v,String why){if(!v)throw new AssertionError(why);checks++;}
 private static void currentRig(){var old=WorldRigCapture.latest;var contacts=PzvrContacts.frame;long now=System.nanoTime();WorldRigCapture.latest=new WorldRigFrame(now,old.inputTime(),old.actor(),old.scene(),old.camera(),old.generation(),old.mouth(),old.hands());PzvrContacts.frame=new ContactFrame(now,contacts.inputTime(),contacts.hands());}
 public static void main(String[] args){
  boolean baseline=args.length>0;var p=NativeDoorCaptureTest.player();var door=NativeDoorCaptureTest.door(p);var frame=new Frame();var mesh=NativeDoorCaptureTest.mesh();WorldContactCapture.requestItems(true);WorldContactCapture.scope(p);RouteModes.world(true);NativeDoorCapture.clear();
  var binding=new viewpoint.packs.PackBind();binding.model=new viewpoint.render.PackModel(new java.io.File("fixtures_doors_02_18.obj"));door.sprite.binding=binding;PackedModelGeometry.store(binding.model,new float[]{-.5f,0,-.5f,.5f,0,-.5f,.5f,2,-.5f,-.5f,2,-.5f},new int[]{0,1,2,0,2,3},3);
  check(!CounterTapBindings.potential(door),"unnamed native sprite stays outside emitted counter classification");door.sprite.name="fixtures_doors_02_18";check(!CounterTapBindings.potential(door),"native door sprite is not reclassified as a counter sink");
  long now=System.nanoTime();WorldRigCapture.latest=new WorldRigFrame(now,500,p,frame.scene,new ContactFrame.Point(20,30,Math.sqrt(6)),1,new ContactFrame.Point(10,20,4),List.of());PzvrContacts.frame=new ContactFrame(now,500,List.of());var furniture=new FurnitureContacts();
  DoorMeshCpu.capture(null,mesh);var rows=furniture.sample(p);boolean leaked=rows.stream().anyMatch(row->row.key().startsWith("packed:"));
  if(baseline){check(leaked,"previous frozen ownership leaked generic packed leaf when native CPU was unavailable");System.out.println("Baseline native door packed fallback: "+checks+" reproduced");return;}
  check(rows.isEmpty(),"selected native door with no successful capture does not regain packed substitute");check(NativeDoorCapture.owns(door),"renderer ownership exists independently of CPU geometry admission");
  // Cold CPU/owner discovery is independent of the current tracked publication consumed below.
  DoorMeshCpu.capture(DoorMeshCpuTest.processed(),mesh);NativeDoorCaptureTest.offer(door,frame,mesh,DoorMeshCpuTest.identity());currentRig();rows=furniture.sample(p);check(rows.size()==1&&rows.getFirst().key().startsWith("native-door:"),"fresh original leaf suppresses packed duplicate");
  door.open=true;check(furniture.sample(p).isEmpty(),"opening invalidates exact shape without restoring invisible packed leaf");check(NativeDoorCapture.owns(door),"opening preserves selected producer ownership");
  var track=new AnimationTrack();track.time=.5f;IsoObjectAnimations.set(door,track);NativeDoorCaptureTest.offer(door,frame,mesh,DoorMeshCpuTest.open());currentRig();check(furniture.sample(p).size()==1,"fresh open leaf admitted");track.time=.6f;check(furniture.sample(p).isEmpty(),"advanced animation cannot regain generic closed barrier");
  NativeDoorCaptureTest.offer(door,frame,mesh,DoorMeshCpuTest.open());DoorMeshCpu.capture(null,mesh);check(furniture.sample(p).isEmpty(),"failed reload retires exact shape and suppresses packed substitute");
  door.spriteModelInit=null;check(furniture.sample(p).isEmpty(),"unready selected sprite-model cache never regains stale packed leaf");door.spriteModelInit=door.sprite;door.spriteModel=null;check(furniture.sample(p).isEmpty(),"selected but missing cached native model never regains packed leaf");
  var level=viewpoint.world.ChunkCache.level(p.getSquare().chunk,1);level.modelled.clear();rows=furniture.sample(p);check(rows.size()==1&&rows.getFirst().key().startsWith("packed:"),"original renderer path change releases native suppression");
  level.modelled.add(door);door.destroyed=true;check(furniture.sample(p).isEmpty(),"destroyed selected native source cannot regain packed ghost");door.destroyed=false;door.removed=true;check(!NativeDoorCapture.owns(door),"removed original owner has no selection lease");door.removed=false;
  level.modelsNear=false;check(!NativeDoorCapture.owns(door),"original near-model mode off releases native ownership");level.modelsNear=true;level.evicted=true;check(!NativeDoorCapture.owns(door),"evicted native selection released");level.evicted=false;
  level.modelled.clear();for(int i=0;i<512;i++)level.modelled.add(new zombie.iso.IsoObject(p.getSquare()));level.modelled.add(door);check(furniture.sample(p).stream().noneMatch(row->row.key().startsWith("packed:")),"unsupported selection density never restores invisible packed leaf");level.modelled.clear();level.modelled.add(door);
  check(zombie.iso.IsoObject.lazyCalls==0&&IsoObjectAnimations.builds==0,"packed suppression never invokes model or animation factories");
  System.out.println("Native door packed fallback: "+checks+" checks passed");
 }
}
