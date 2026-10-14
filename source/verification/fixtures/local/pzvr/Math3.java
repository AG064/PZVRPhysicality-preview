package local.pzvr;
/** Authored API double, not copied upstream implementation. */
public final class Math3 {
 public record Vec(double x,double y,double z){public Vec add(Vec other){return new Vec(x+other.x,y+other.y,z+other.z);}}
 public record Quat(double x,double y,double z,double w){public Vec rotate(Vec point){var value=new pzvrphysicality.MouthMath.Rotation(x,y,z,w).rotate(new pzvrphysicality.MouthMath.Vec(point.x,point.y,point.z));return new Vec(value.x(),value.y(),value.z());}}
 public record Pose(Vec position,Quat rotation){}
}
