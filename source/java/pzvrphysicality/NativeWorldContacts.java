package pzvrphysicality;

import java.util.*;
import viewpoint.world.ChunkCache;
import viewpointvr.physics.RigidPhysics;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoChunk;

/** Read-only admission of completed generated barriers into the existing physical environment. */
public final class NativeWorldContacts {
 private int exact,missing,stale,unsupported,omitted;private long generation=-1;
 public List<PhysicalWorld.Static> sample(IsoPlayer player){
  if(!WorldContactCapture.enabled()||!WorldContactCapture.ownerThread())return List.of();exact=missing=stale=unsupported=omitted=0;
  if(!WorldContactCapture.eligible(player)||zombie.GameTime.isGamePaused())return List.of();WorldContactCapture.scope(player);
  long now=System.nanoTime();var rig=WorldRigCapture.latest;var contacts=PzvrContacts.sample(now);
  if(rig==null||!rig.fresh(now)||rig.actor()!=player||rig.scene()==null||contacts==null||!contacts.fresh(now)||contacts.inputTime()!=rig.inputTime())return List.of();
  if(generation!=rig.generation())generation=rig.generation();
  var result=new ArrayList<PhysicalWorld.Static>(NativeDoorCapture.sample(player));var visited=Collections.newSetFromMap(new IdentityHashMap<IsoChunk,Boolean>());int x=(int)Math.floor(player.getX()),y=(int)Math.floor(player.getY()),z=(int)Math.floor(player.getZ()),triangles=result.stream().mapToInt(row->row.shape().faces().length/3).sum();
  for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++){
   var square=player.getCell().getGridSquare(x+dx,y+dy,z);if(square==null||square.getChunk()==null||!visited.add(square.getChunk()))continue;var chunk=square.getChunk();
   try{
    var data=ChunkCache.meshOn(square);if(data==null){missing++;continue;}if(!WorldContactCapture.current(chunk,z,data)){stale++;continue;}
    var copied=WorldContactCapture.get(data);if(copied==null)copied=WorldContactCapture.fallback(chunk,z,data);if(copied==null){missing++;continue;}var receipt=copied.receipt();
    if(!WorldContactCapture.matches(receipt)){stale++;continue;}omitted+=copied.omitted();
    if(copied.unowned()!=null){
     boolean current=true;for(int index=0;index<64;index++){var state=receipt.squares().get(index);var nativeSquare=chunk.getGridSquare(index%8,index/8,z);if(state==null?nativeSquare!=null:!state.matches(nativeSquare,-1)||nativeSquare!=player.getCell().getGridSquare(state.x(),state.y(),z)){current=false;break;}}
     if(!current)stale++;else{int faces=copied.unowned().faces().length/3;if(result.size()<64&&triangles+faces<=60000){result.add(new PhysicalWorld.Static("generated-static:"+receipt.wx()+":"+receipt.wy()+":"+z,copied.unowned(),RigidPhysics.pose(receipt.wx()*8+4,receipt.wy()*8+4,(float)(z*WorldContactCapture.STOREY))));triangles+=faces;exact++;}else unsupported++;}
    }
    for(var e:copied.shapes().entrySet()){
     int owner=e.getKey(),index=owner>>2;var state=receipt.squares().get(index);if(state==null||Math.abs(state.x()-x)>2||Math.abs(state.y()-y)>2)continue;
     var current=chunk.getGridSquare(index%8,index/8,z);if(!state.matches(current,owner&3)||current!=player.getCell().getGridSquare(state.x(),state.y(),z)){stale++;continue;}
     var shape=e.getValue();int faces=shape.faces().length/3;if(result.size()>=64||triangles+faces>60000){unsupported++;continue;}
     result.add(new PhysicalWorld.Static("generated:"+state.x()+":"+state.y()+":"+z+":"+(owner&3),shape,RigidPhysics.pose(state.x()+.5f,state.y()+.5f,(float)(z*WorldContactCapture.STOREY))));triangles+=faces;exact++;
    }
   }catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){unsupported++;}
  }
  long end=System.nanoTime();var finalContacts=PzvrContacts.sample(end);
  if(WorldRigCapture.latest!=rig||!rig.fresh(end)||finalContacts==null||!finalContacts.fresh(end)||finalContacts.inputTime()!=rig.inputTime()||!WorldContactCapture.eligible(player))return List.of();return result;
 }
 public void clear(){exact=missing=stale=unsupported=omitted=0;generation=-1;}
 public boolean owns(zombie.iso.IsoObject object){return WorldContactCapture.enabled()&&NativeDoorCapture.owns(object);}
 public String status(){return "generated contacts "+exact+", missing "+missing+", stale "+stale+", unsupported "+unsupported+", omitted owner runs "+omitted+"; "+WorldContactCapture.status()+"; "+(WorldContactCapture.enabled()?NativeDoorCapture.status():"native door contacts idle");}
}
