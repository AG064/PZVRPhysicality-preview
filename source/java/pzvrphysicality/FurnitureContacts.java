package pzvrphysicality;

import java.util.*;
import viewpoint.packs.ModelPacks;
import viewpoint.packs.PackBind;
import viewpoint.render.PackModel;
import viewpoint.world.ChunkCache;
import viewpoint.world.WorldMesher;
import viewpointvr.physics.PackedModelGeometry;
import viewpointvr.physics.RigidPhysics;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoObject;

/** Bounded, game-thread discovery around loose items. Does not depend on looking at the furniture. */
public final class FurnitureContacts {
 private record Fitted(PackModel model,PackedModelGeometry.Mesh mesh,float cos,float sin,float scale,viewpointvr.physics.CollisionShape shape){}
 private final Map<IsoObject,Fitted> cache=new WeakHashMap<>();
 private NativeWindowContacts windows;private boolean windowChecked;
 private NativeWorldContacts generated;
 private NativeWindowContacts windows(){
  if(!windowChecked){windowChecked=true;try{
   Class.forName("techjungle.betterwindow.WindowAssets",false,FurnitureContacts.class.getClassLoader());
   windows=new NativeWindowContacts();
  }catch(ClassNotFoundException|LinkageError unavailable){windows=null;}}
  return windows;
 }
 private int exact,missing,skipped;private long warning;
 public List<PhysicalWorld.Static> sample(IsoPlayer player) {
  var windowContacts=windows();var result=new ArrayList<PhysicalWorld.Static>(windowContacts==null?List.of():windowContacts.sample(player));exact=missing=skipped=0;
  if(RouteModes.world()){if(generated==null)generated=new NativeWorldContacts();for(var row:generated.sample(player)){if(result.size()>=64)break;result.add(row);}}
  int x=(int)Math.floor(player.getX()),y=(int)Math.floor(player.getY()),z=(int)Math.floor(player.getZ()),scanned=0,triangles=0;
  outer:for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++){
   var square=player.getCell().getGridSquare(x+dx,y+dy,z);if(square==null||!ChunkCache.packsOn(square))continue;
   var objects=square.getObjects();
   for(int index=0;index<objects.size();index++){
    if(++scanned>512||result.size()>=128)break outer;
    var object=objects.get(index);if(object==null||object.getSquare()!=square||object.getSprite()==null||windowContacts!=null&&windowContacts.owns(object)||RouteModes.world()&&generated!=null&&generated.owns(object))continue;
    try{
     var observed=ShelfBridge.binding(object);var binding=observed==null?ModelPacks.bind(object.getSprite()):observed.binding();if(binding==null)continue;
     int phase=binding.model.state();if(phase!=PackModel.READ&&phase!=PackModel.RESIDENT){missing++;continue;}
     var mesh=PackedModelGeometry.get(binding.model);
     if(mesh==null){MeshRecovery.request(binding.model);missing++;continue;}
     int count=mesh.faces().length/3;if(count>20000||triangles+count>60000){skipped++;continue;}
     boolean turns=observed==null?PackBind.turns(object.getSprite()):observed.turns();float c=turns?binding.cos:1,s=turns?binding.sin:0;
     var old=cache.get(object);
     var shape=old!=null&&old.model==binding.model&&old.mesh==mesh&&old.cos==c&&old.sin==s&&old.scale==binding.scale
       ?old.shape:MeshPlacement.place(mesh,c,s,binding.scale);
     if(cache.size()>=128&&!cache.containsKey(object))cache.clear();
     cache.put(object,new Fitted(binding.model,mesh,c,s,binding.scale,shape));
     var pose=RigidPhysics.pose(square.getX()+.5f+binding.x,square.getY()+.5f+binding.y,
       (float)(square.getZ()*Math.sqrt(6))+WorldMesher.rise(object)+binding.z);
     result.add(new PhysicalWorld.Static("packed:"+square.getX()+":"+square.getY()+":"+z+":"+index,shape,pose));exact++;triangles+=count;
    }catch(RuntimeException unsupported){skipped++;long now=System.nanoTime();if(now-warning>5_000_000_000L){warning=now;System.err.println("[PZVR Physicality] Furniture contact skipped: "+unsupported);}}
   }
  }
  return result;
 }
 public String status(){return "packed contacts "+exact+", missing meshes "+missing+", skipped "+skipped+"; "+(windows==null?"window contacts "+(windowChecked?"unavailable":"idle"):windows.status())+"; "+(generated==null?"generated contacts idle":generated.status());}
 public void clear(){cache.clear();if(windows!=null)windows.clear();if(generated!=null)generated.clear();windowChecked=false;exact=missing=skipped=0;}
}
