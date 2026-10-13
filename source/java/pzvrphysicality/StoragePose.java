package pzvrphysicality;
import org.joml.Matrix3f;import org.joml.Quaternionf;

/** World-space body poses and the exact inverse of the stored normalized interior mapping. */
public final class StoragePose {
 public static float[] hand(WorldRigFrame.Hand hand){
  double scale=hand.units();var x=hand.right();var y=hand.forward();var z=hand.up();
  var basis=new Matrix3f((float)(-x.x()/scale),(float)(-x.y()/scale),(float)(-x.height()/scale),
   (float)(-y.x()/scale),(float)(-y.y()/scale),(float)(-y.height()/scale),
   (float)(z.x()/scale),(float)(z.y()/scale),(float)(z.height()/scale));
  if(!basis.isFinite()||Math.abs(basis.determinant()-1)>.005)throw new IllegalArgumentException("Storage hand basis");
  var q=basis.getNormalizedRotation(new Quaternionf()).normalize();var p=hand.grip();return new float[]{(float)p.x(),(float)p.y(),(float)p.height(),q.x,q.y,q.z,q.w};
 }
 public static float[] normalized(StorageFrame frame,float[] world){
  if(frame==null||world==null||world.length<7)return null;for(int n=0;n<7;n++)if(!Float.isFinite(world[n]))return null;
  double norm=world[3]*world[3]+world[4]*world[4]+world[5]*world[5]+world[6]*world[6];if(Math.abs(norm-1)>.002)return null;
  var local=frame.local(new ContactFrame.Point(world[0],world[1],world[2]));double inset=0;
  double x=(local.x()-frame.minX()-inset)/(frame.maxX()-frame.minX()-2*inset),y=(local.y()-frame.minD()-inset)/(frame.maxD()-frame.minD()-2*inset),z=(local.height()-frame.minH()-inset)/(frame.maxH()-frame.minH()-2*inset);
  if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||x<-.05||x>1.05||y<-.05||y>1.05||z<-.05||z>1.05)return null;
  var p=frame.rotation(0,0,0,1);var q=new Quaternionf(p[0],p[1],p[2],p[3]).invert().mul(new Quaternionf(world[3],world[4],world[5],world[6])).normalize();
  return new float[]{(float)x,(float)y,(float)z,q.x,q.y,q.z,q.w};
 }
 private StoragePose(){}
}
