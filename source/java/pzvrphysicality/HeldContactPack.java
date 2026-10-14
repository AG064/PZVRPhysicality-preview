package pzvrphysicality;

import java.util.*;import org.joml.Quaternionf;import viewpoint.render.SceneData;import zombie.characters.IsoPlayer;

/** Separate real pack-mesh receipt. It never substitutes a native model instance or socket record. */
public final class HeldContactPack {
 private static final HeldContactExternal.Source source=new HeldContactExternal.Source(){
  public List<HeldContactExternal.Offer> targets(viewpoint.core.Frame frame,IsoPlayer actor,long now){return HeldContactPack.targets(frame.scene,actor,now).stream().map(HeldContactPack::offer).toList();}
  public boolean current(HeldContactExternal.Offer offer,long now){return offer.source()==this&&offer.receipt() instanceof Candidate candidate&&offer.item()==candidate.target.item()&&offer.actor()==candidate.target.actor()&&offer.side()==candidate.target.side()&&offer.scene()==candidate.target.scene()&&offer.input()==candidate.target.input()&&offer.rigGeneration()==candidate.target.rigGeneration()&&offer.sourceEpoch()==candidate.target.modeEpoch()&&offer.geometryIdentity()==candidate.target.geometry()&&offer.shape()==candidate.target.shape()&&Arrays.equals(offer.pose(),candidate.target.pose())&&HeldContactPack.current(candidate,now);}
  public boolean corrected(HeldContactExternal.Offer offer,float[] pose){return current(offer,System.nanoTime())&&MoveableCarry.corrected(((Candidate)offer.receipt()).target,pose);}
  public void clear(){HeldContactPack.clear();}
 };
 public static HeldContactExternal.Source source(){return source;}
 private static HeldContactExternal.Offer offer(Candidate candidate){var target=candidate.target;return new HeldContactExternal.Offer(source,candidate,target.actor(),target.item(),target.side(),target.scene(),target.input(),target.rigGeneration(),target.modeEpoch(),target.time(),target.units(),target.mass(),target.shape(),target.geometry(),target.pose());}
 public record Candidate(MoveableCarry.Target target){public Candidate{Objects.requireNonNull(target);}}
 public static List<Candidate> targets(SceneData scene,IsoPlayer actor,long now){
  if(!HeldContactDriver.enabled())return List.of();var result=new ArrayList<Candidate>();
  for(var target:MoveableCarry.targets(scene,actor,now))if(HeldContactCapture.prop(actor,target.item(),target.side())&&MoveableCarry.current(target,now))result.add(new Candidate(target));
  return List.copyOf(result);
 }
 public static boolean current(Candidate candidate,long now){return candidate!=null&&HeldContactDriver.enabled()&&HeldContactCapture.prop(candidate.target.actor(),candidate.target.item(),candidate.target.side())&&MoveableCarry.current(candidate.target,now);}
 /** Apply the collision displacement and composed rotation relative to the collision-free reference. */
 public static boolean constrain(Candidate candidate,HeldContactWorld.Response response,double metres){
  if(!current(candidate,System.nanoTime())||response==null||!Double.isFinite(metres)||metres<=0)return false;
  var target=candidate.target;var shift=response.translation();double length=0;for(float value:shift){if(!Float.isFinite(value))return false;length+=value*value;}
  double limit=Math.min(.0399,metres)*target.units(),scale=length>limit*limit?limit/Math.sqrt(length):1;var pose=target.pose();for(int n=0;n<3;n++)pose[n]+=(float)(shift[n]*scale);
  var delta=HeldContactRotation.difference(response);if(delta==null)return false;double angle=angle(response),maximum=Math.toRadians(14.9);if(angle>maximum)delta=new Quaternionf().slerp(delta,(float)(maximum/angle));var desired=new Quaternionf(pose[3],pose[4],pose[5],pose[6]);delta.mul(desired).normalize();pose[3]=delta.x;pose[4]=delta.y;pose[5]=delta.z;pose[6]=delta.w;
  return MoveableCarry.corrected(target,pose);
 }
 public static double angle(HeldContactWorld.Response response){return HeldContactRotation.angle(response);}
 public static void clear(){MoveableCarry.clearCorrections();}
 private HeldContactPack(){}
}
