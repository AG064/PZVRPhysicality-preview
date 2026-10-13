package pzvrphysicality;

/** Physical metres in raw OpenXR space. Game world scale and camera yaw never enter mouth reach. */
public final class MouthMath {
 public record Vec(double x,double y,double z){
  public Vec{if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Physical point");}
  public Vec add(Vec other){return new Vec(x+other.x,y+other.y,z+other.z);}
  public double distance(Vec other){return Math.sqrt((x-other.x)*(x-other.x)+(y-other.y)*(y-other.y)+(z-other.z)*(z-other.z));}
 }
 public record Rotation(double x,double y,double z,double w){
  public Rotation{double n=x*x+y*y+z*z+w*w;if(!Double.isFinite(n)||Math.abs(n-1)>.005)throw new IllegalArgumentException("Physical orientation");}
  public Vec rotate(Vec v){double a=2*(y*v.z-z*v.y),b=2*(z*v.x-x*v.z),c=2*(x*v.y-y*v.x);return new Vec(v.x+w*a+y*c-z*b,v.y+w*b+z*a-x*c,v.z+w*c+x*b-y*a);}
 }
 public record Pose(Vec position,Rotation rotation){public Pose{if(position==null||rotation==null)throw new IllegalArgumentException("Physical pose");}public Vec point(Vec offset){return position.add(rotation.rotate(offset));}}
 public record Profile(Vec contact,Vec up,double maximumUpDot){
  public Profile{if(contact==null||up==null||contact.distance(new Vec(0,0,0))>.4||Math.abs(up.x*up.x+up.y*up.y+up.z*up.z-1)>.002||!Double.isFinite(maximumUpDot)||maximumUpDot< -1||maximumUpDot>1)throw new IllegalArgumentException("Mouth profile");}
 }
 public static final Profile FOOD=new Profile(new Vec(0,0,-.06),new Vec(0,1,0),1);
 public static final Profile BOTTLE=new Profile(new Vec(0,.13,0),new Vec(0,1,0),.25);
 public static final Profile CUP=new Profile(new Vec(0,.075,-.025),new Vec(0,1,0),.65);
 public static boolean contact(Pose head,Pose hand,Profile profile,double mouthDown,double radius){
  if(head==null||hand==null||profile==null||!Double.isFinite(mouthDown)||mouthDown<.04||mouthDown>.20||!Double.isFinite(radius)||radius<.05||radius>.16)return false;
  return head.point(new Vec(0,-mouthDown,-.08)).distance(hand.point(profile.contact))<=radius;
 }
 public static boolean flowing(Pose hand,Profile profile){return hand!=null&&profile!=null&&hand.rotation.rotate(profile.up).y<=profile.maximumUpDot;}
 /** Food's percentage is measured against original base hunger, not the fraction currently remaining. */
 public static double biteFraction(double baseHunger){return !Double.isFinite(baseHunger)||baseHunger>=0?0:Math.min(1,.015/Math.abs(baseHunger));}
 /** Native fluid quantities are litres. Each sip reserves at most 30 ml of the current amount. */
 public static double sipFraction(double amount){return !Double.isFinite(amount)||amount<=0?0:Math.min(1,.03/amount);}
 private MouthMath(){}
}
