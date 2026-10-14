package pzvrphysicality;
import java.lang.ref.WeakReference;import org.joml.Matrix4f;

/** Physical pickup provenance and exact grab-relative matrices, with no native item clones or pooled model handles. */
public final class MoveableCarryPose {
 public record Provenance(Object actor,Object item,int side,long epoch,long rigGeneration,long sourceGeneration){public Provenance{if(actor==null||item==null||side<0||side>1||epoch<0||rigGeneration<0||sourceGeneration<0)throw new IllegalArgumentException("Moveable carry provenance");}}
 private record Reserved(WeakReference<Object> actor,WeakReference<Object> item,int side,long epoch,long rigGeneration,long sourceGeneration,long time,float[] relative){Reserved{relative=relative.clone();}}
 private Reserved pending,held;
 public synchronized void reserve(Provenance p,float[] itemPose,float[] handPose,long now){if(now<0)throw new IllegalArgumentException("Moveable carry time");var relative=MoveableGeometry.world(handPose).invert().mul(MoveableGeometry.world(itemPose));var values=new float[16];relative.get(values);pending=new Reserved(new WeakReference<>(p.actor),new WeakReference<>(p.item),p.side,p.epoch,p.rigGeneration,p.sourceGeneration,now,values);}
 private static boolean matches(Reserved r,Provenance p){return r!=null&&p!=null&&r.actor.get()==p.actor&&r.item.get()==p.item&&r.side==p.side&&r.epoch==p.epoch&&r.rigGeneration==p.rigGeneration&&r.sourceGeneration==p.sourceGeneration;}
 public synchronized boolean commit(Provenance p,long now){if(!matches(pending,p)||now<pending.time||now-pending.time>10_000_000_000L){pending=null;return false;}held=pending;pending=null;return true;}
 public synchronized float[] pose(Provenance p,float[] handPose){if(!matches(held,p)){held=null;return null;}return MoveableGeometry.pose(MoveableGeometry.world(handPose).mul(new Matrix4f().set(held.relative)));}
 public synchronized void clear(){pending=held=null;}
 public synchronized void cancelPending(){pending=null;}
 public synchronized boolean held(Object item){return held!=null&&held.item.get()==item;}
 /** Map the request's immutable XR grip through the matching current native hand/world mapping. */
 public static float[] requestedHand(float[] currentWorld,float[] currentRaw,float[] requestedRaw,double units){if(!Double.isFinite(units)||units<.25||units>4)throw new IllegalArgumentException("Moveable hand scale");var basis=new Matrix4f(-1,0,0,0,0,0,-1,0,0,1,0,0,0,0,0,1);var rawNow=MoveableGeometry.world(currentRaw).mul(basis);var rawThen=MoveableGeometry.world(requestedRaw).mul(basis);var relative=rawNow.invert().mul(rawThen);relative.m30((float)(relative.m30()*units)).m31((float)(relative.m31()*units)).m32((float)(relative.m32()*units));return MoveableGeometry.pose(MoveableGeometry.world(currentWorld).mul(relative));}
}
