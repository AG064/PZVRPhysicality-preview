package local.pzvr;
/** Descriptor-compatible numeric records. Their values never invoke upstream pose math. */
public final class Math3 {
 public record Vec(double x,double y,double z){}
 public record Quat(double x,double y,double z,double w){}
 public record Pose(Vec position,Quat rotation){public static final Pose IDENTITY=new Pose(new Vec(0,0,0),new Quat(0,0,0,1));}
 private Math3(){}
}
