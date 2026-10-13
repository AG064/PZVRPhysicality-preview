package pzvrphysicality;

import java.nio.file.*;import java.security.MessageDigest;import java.util.*;
import viewpoint.packs.*;import viewpoint.render.PackModel;import viewpoint.world.*;
import zombie.characters.IsoPlayer;import zombie.iso.IsoObject;

/** Exact selected-model discovery around the player and hands, with native blocking/owner checks. */
public final class NearbyTaps {
 public record Target(IsoObject source,PackModel model,TapProfile profile,ContactFrame.Point base,float cos,float sin,float scale,List<ContactFrame.Point> handles,ContactFrame.Point spout){public Target{handles=List.copyOf(handles);}}
 private record Cached(long bytes,long modified,TapProfile profile){}
 private final Map<PackModel,Cached> checked=new WeakHashMap<>();private int unsupported;private long warning;
 private TapProfile profile(PackModel model)throws Exception{
  if(model.state()!=PackModel.READ&&model.state()!=PackModel.RESIDENT)return null;
  var known=TapProfile.find(model.obj.getName());if(known==null)return null;
  Path file=model.obj.toPath();var attributes=Files.readAttributes(file,java.nio.file.attribute.BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
  if(!attributes.isRegularFile()||attributes.size()<=0||attributes.size()>2*1024*1024)return null;
  var previous=checked.get(model);
  if(previous!=null&&previous.bytes==attributes.size()&&previous.modified==attributes.lastModifiedTime().toMillis())return previous.profile!=null&&previous.profile.matches(model.bounds())?previous.profile:null;
  var digest=MessageDigest.getInstance("SHA-256");try(var stream=Files.newInputStream(file)){var buffer=new byte[16384];int read;long count=0;while((read=stream.read(buffer))!=-1){if((count+=read)>2*1024*1024)throw new IllegalArgumentException("Tap model grew during read");digest.update(buffer,0,read);}}
  var after=Files.readAttributes(file,java.nio.file.attribute.BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
  if(after.size()!=attributes.size()||!after.lastModifiedTime().equals(attributes.lastModifiedTime())||!Objects.equals(after.fileKey(),attributes.fileKey()))return null;
  TapProfile accepted=known.sha256().equals(HexFormat.of().formatHex(digest.digest()))&&known.matches(model.bounds())?known:null;
  if(checked.size()>=64&&!checked.containsKey(model))checked.clear();checked.put(model,new Cached(attributes.size(),attributes.lastModifiedTime().toMillis(),accepted));return accepted;
 }
 public List<Target> sample(IsoPlayer player,WorldRigFrame rig){
  if(player==null||rig==null||rig.actor()!=player||player.getSquare()==null)return List.of();var targets=new ArrayList<Target>();unsupported=0;
  var squares=new LinkedHashSet<String>();var centres=new ArrayList<ContactFrame.Point>();centres.add(new ContactFrame.Point(player.getX(),player.getY(),player.getZ()*Math.sqrt(6)));for(var hand:rig.hands())centres.add(hand.grip());
  int scanned=0;outer:for(var centre:centres)for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){
   int x=(int)Math.floor(centre.x())+dx,y=(int)Math.floor(centre.y())+dy,z=player.getSquare().getZ();if(!squares.add(x+":"+y+":"+z))continue;
   var square=player.getCell().getGridSquare(x,y,z);if(square==null||!square.isAdjacentTo(player.getSquare())||square.isBlockedTo(player.getSquare())||!ChunkCache.packsOn(square))continue;
   var objects=square.getObjects();for(int i=0;i<objects.size();i++){
    if(++scanned>256||targets.size()>=8)break outer;var object=objects.get(i);if(object==null||object.getSquare()!=square||object.getSprite()==null||object.getObjectIndex()<0)continue;
    try{
     var bind=ModelPacks.bind(object.getSprite());if(bind==null)continue;var profile=profile(bind.model);if(profile==null){if(object.hasWater())unsupported++;continue;}
     var base=new ContactFrame.Point(square.getX()+.5+bind.x,square.getY()+.5+bind.y,square.getZ()*Math.sqrt(6)+WorldMesher.rise(object)+bind.z);
     boolean turn=PackBind.turns(object.getSprite());float c=turn?bind.cos:1,s=turn?bind.sin:0;
     var handles=profile.handles().stream().map(point->profile.placed(point,base,c,s,bind.scale)).toList();var spout=profile.placed(profile.spout(),base,c,s,bind.scale);
     targets.add(new Target(object,bind.model,profile,base,c,s,bind.scale,handles,spout));
    }catch(Exception error){long now=System.nanoTime();if(now-warning>5_000_000_000L){warning=now;System.err.println("[PZVR Physicality] Tap profile unavailable: "+error);}}
   }
  }
  return targets;
 }
 public int unsupported(){return unsupported;}
 public void clear(){checked.clear();unsupported=0;}
}
