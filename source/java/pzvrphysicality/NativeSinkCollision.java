package pzvrphysicality;

import zombie.characters.IsoPlayer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;

/** Refines one proven standalone sink footprint after the original native collision routine finishes. */
public final class NativeSinkCollision {
 private static final java.util.concurrent.atomic.LongAdder refined=new java.util.concurrent.atomic.LongAdder(),rejected=new java.util.concurrent.atomic.LongAdder();private static volatile String detail="No native sink refinement";
 private static boolean objects(IsoGridSquare square,IsoObject sink){var list=square.getObjects();if(list.size()>128)return false;
  int owners=0;for(var object:list){if(object==null||object.getClass()!=IsoObject.class||object.getSquare()!=square||object.getProperties()==null)return false;
   var properties=object.getProperties();if(properties.has(IsoFlagType.solid))return false;
   if(properties.has(IsoFlagType.solidtrans)){if(object!=sink)return false;owners++;}}
  return sink==null?owners==0:owners==1;
 }
 private static boolean ordinary(IsoGridSquare square,IsoPlayer player){
  if(square==null||square.collideMatrix==-1||!square.solidFloorCached||!square.solidFloor||square.isSolid()||square.HasStairs()||square.HasStairsBelow()||square.hasSlopedSurface()||square.getProperties().has(IsoFlagType.water)
   ||!square.getSpecialObjects().isEmpty()||square.getMovingObjects().size()>64||square.getChunk()==null||!square.getChunk().vehicles.isEmpty())return false;
  for(var mover:square.getMovingObjects())if(mover!=player)return false;return true;
 }
 public static boolean refine(Object value,Object moving,int dx,int dy,int dz,boolean original,boolean completed){
  if(!NativePlayerCollision.enabled()||!original||!completed)return original;
  if(!(value instanceof IsoGridSquare source)||!(moving instanceof IsoPlayer player)||dz!=0||Math.abs(dx)+Math.abs(dy)!=1)return original;
  try{
   var contacts=NativePlayerCollision.sinkContacts(player,System.nanoTime());if(contacts==null)return original;
   var target=player.getCell().getGridSquare(source.getX()+dx,source.getY()+dy,source.getZ());
   if(player.isNoClip()||player.getCollidedObject()!=null||player.isCollidedWithDoor()||player.isCollidedWithVehicle()||player.getLastSquare()!=source
    ||player.getCell().getGridSquare(source.getX(),source.getY(),source.getZ())!=source||target==null||target.getCell()!=player.getCell()
    ||zombie.iso.IsoWorld.instance.getCell()!=player.getCell()||!ordinary(source,player)||!ordinary(target,player)||!target.isSolidTrans()||!objects(source,null)
    ||!zombie.iso.IsoWorld.instance.metaGrid.isValidChunk(target.getX()/8,target.getY()/8))return original;
   double x=player.getLastX(),y=player.getLastY(),z=player.getZ(),mx=player.getNextX()-x,my=player.getNextY()-y;
   if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Double.isFinite(mx)||!Double.isFinite(my)||Math.hypot(mx,my)>.5||mx*dx+my*dy<=1e-7
    ||Math.abs(z-Math.rint(z))>.001||Math.abs(player.getLastZ()-z)>.001||Math.floor(x)!=source.getX()||Math.floor(y)!=source.getY()||Math.floor(z)!=source.getZ()
    ||player.getCurrentSquare()!=source&&player.getCurrentSquare()!=target||Math.floor(player.getNextX())!=player.getCurrentSquare().getX()||Math.floor(player.getNextY())!=player.getCurrentSquare().getY())return original;
   IsoObject sink=null;for(var object:target.getObjects())if(object!=null&&object.getProperties()!=null&&object.getProperties().has(IsoFlagType.solidtrans)){if(sink!=null)return original;sink=object;}
   if(sink==null||!objects(target,sink))return original;var receipt=contacts.contact(sink);
   if(receipt==null||receipt.source()!=sink||receipt.square()!=target||receipt.sinkProfile()==null||receipt.sinkProfile().authored()||!contacts.current(receipt))return original;
   if(!NativePlayerCollision.sinkPathClear(player,receipt.row(),x,y,mx,my)){rejected.increment();detail="Verified sink mesh still obstructs avatar";return original;}
   // Both floors are already cached and this pair is cardinal, so these exact SDK queries neither recalc nor fill caches.
   if(!source.CalculateCollide(target,false,false,false)||source.CalculateCollide(target,false,false,true))return original;
   if(NativePlayerCollision.sinkContacts(player,System.nanoTime())!=contacts||!contacts.current(receipt)||!ordinary(source,player)||!ordinary(target,player)||!objects(source,null)||!objects(target,sink)
    ||player.getCollidedObject()!=null||player.isCollidedWithDoor()||player.isCollidedWithVehicle())return original;
   refined.increment();detail="Native standalone sink footprint replaced by clear captured mesh path";return false;
  }catch(RuntimeException|LinkageError unsupported){rejected.increment();detail="Native sink refinement omitted: "+unsupported.getClass().getSimpleName();return original;}
 }
 public static String status(){return detail+"; refined="+refined.sum()+" rejected="+rejected.sum();}
 static void reset(){refined.reset();rejected.reset();detail="No native sink refinement";}
 private NativeSinkCollision(){}
}
