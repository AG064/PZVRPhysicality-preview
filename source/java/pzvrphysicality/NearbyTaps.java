package pzvrphysicality;

import java.nio.file.*;import java.security.MessageDigest;import java.util.*;
import viewpoint.packs.*;import viewpoint.render.PackModel;import viewpoint.world.*;
import zombie.characters.IsoPlayer;import zombie.iso.IsoObject;

/** Exact selected-model discovery around the player and hands, with native blocking/owner checks. */
public final class NearbyTaps {
 public record Target(IsoObject source,IsoObject owner,PackModel model,TapProfile profile,ContactFrame.Point base,float cos,float sin,float scale,List<ContactFrame.Point> handles,ContactFrame.Point spout){public Target{handles=List.copyOf(handles);}}
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
   var square=player.getCell().getGridSquare(x,y,z);if(square==null||square!=player.getSquare()&&(!square.isAdjacentTo(player.getSquare())||!player.getSquare().canReachTo(square))||!ChunkCache.packsOn(square))continue;
   var objects=square.getObjects();for(int i=0;i<objects.size();i++){
    if(++scanned>256||targets.size()>=8)break outer;var object=objects.get(i);if(object==null||object.getSquare()!=square||object.getSprite()==null||object.getObjectIndex()<0)continue;
    try{
     var observed=CounterTapBindings.current(object);var bind=observed==null?ModelPacks.bind(object.getSprite()):observed.binding();if(bind==null)continue;var profile=profile(bind.model);if(profile==null){if(object.hasWater())unsupported++;continue;}
     if(profile.authored()&&(observed==null||!WorldVisuals.enabled()))continue;
     IsoObject source=profile.authored()?water(object,profile):object;if(source==null||targets.stream().anyMatch(t->t.source()==source))continue;
     var base=new ContactFrame.Point(square.getX()+.5+bind.x,square.getY()+.5+bind.y,square.getZ()*Math.sqrt(6)+WorldMesher.rise(object)+bind.z);
     boolean turn=observed==null?PackBind.turns(object.getSprite()):observed.turns();float c=turn?bind.cos:1,s=turn?bind.sin:0;
     var handles=profile.handles().stream().map(point->profile.placed(point,base,c,s,bind.scale)).toList();var spout=profile.placed(profile.spout(),base,c,s,bind.scale);
     targets.add(new Target(source,object,bind.model,profile,base,c,s,bind.scale,handles,spout));
    }catch(Exception error){long now=System.nanoTime();if(now-warning>5_000_000_000L){warning=now;System.err.println("[PZVR Physicality] Tap profile unavailable: "+error);}}
   }
  }
  return targets;
 }
 public int unsupported(){return unsupported;}
 /** Source-file/profile verification only. Never asks the engine to create or draw a model. */
 public TapProfile checkedProfile(PackModel model){try{return model==null?null:profile(model);}catch(Exception|LinkageError unsupported){return null;}}
 // Exact selected SinkCounterSelector.profile contract. Counter colour and sink fixture material are independent.
 private static String kind(zombie.iso.sprite.IsoSprite sprite){if(sprite==null||sprite.name==null||!sprite.name.startsWith("fixtures_sinks_01_"))return null;try{int index=Integer.parseInt(sprite.name.substring(18));if(index>=0&&index<=3||index>=20&&index<=23)return "oval";if(index>=4&&index<=11||index>=16&&index<=19)return "rect";}catch(NumberFormatException unsupported){}return null;}
 private static String kind(IsoObject object){String value=kind(object.getSprite());if(value!=null)return value;var attached=object.getAttachedAnimSprite();if(attached!=null){if(attached.size()>16)return null;for(var instance:attached)if(instance!=null&&(value=kind(instance.parentSprite))!=null)return value;}return kind(object.getOverlaySprite());}
 static IsoObject water(IsoObject owner){return water(owner,null);}
 private static IsoObject water(IsoObject owner,TapProfile expected){
  String wanted=expected==null?null:expected.model().contains("_rect.")?"rect":"oval";String own=kind(owner);if(own!=null)return wanted==null||own.equals(wanted)?owner:null;
  var square=owner.getSquare();IsoObject result=null;if(square==null)return null;
  for(var object:square.getObjects())if(object!=null&&object.getSquare()==square&&object.getObjectIndex()>=0){String found=kind(object);if(found!=null&&(wanted==null||wanted.equals(found))){if(result!=null&&result!=object)return null;result=object;}}return result;
 }
 public static boolean current(Target target,IsoPlayer player){
  if(target==null||player==null||player.getSquare()==null)return false;var source=target.source();var owner=target.owner();var square=owner.getSquare();
  if(square==null||owner.getObjectIndex()<0||source.getSquare()!=square||source.getObjectIndex()<0||square.getObjects().indexOf(source)<0||square.getObjects().indexOf(owner)<0||square!=player.getSquare()&&(!square.isAdjacentTo(player.getSquare())||!player.getSquare().canReachTo(square))||owner.getSprite()==null)return false;
  var observed=CounterTapBindings.current(owner);if(target.profile().authored()&&(observed==null||!WorldVisuals.enabled()||water(owner,target.profile())!=source))return false;
  var bind=observed==null?ModelPacks.bind(owner.getSprite()):observed.binding();if(bind==null||bind.model!=target.model()||bind.model.state()!=PackModel.READ&&bind.model.state()!=PackModel.RESIDENT||!target.profile().matches(bind.model.bounds())||!ChunkCache.packsOn(square))return false;
  var base=new ContactFrame.Point(square.getX()+.5+bind.x,square.getY()+.5+bind.y,square.getZ()*Math.sqrt(6)+WorldMesher.rise(owner)+bind.z);boolean turns=observed==null?PackBind.turns(owner.getSprite()):observed.turns();float c=turns?bind.cos:1,s=turns?bind.sin:0;
  return base.distance(target.base())<.0001&&Math.abs(c-target.cos())<.0001&&Math.abs(s-target.sin())<.0001&&Math.abs(bind.scale-target.scale())<.0001;
 }
 public void clear(){checked.clear();unsupported=0;}
}
