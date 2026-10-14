package pzvrphysicality;

import org.joml.Matrix4f;import org.joml.Vector3f;import org.joml.Quaternionf;import viewpoint.packs.PackBind;import viewpointvr.physics.CollisionShape;

/** One canonical detached mesh drives both the full-matrix visual and its exact native physics shape. */
public final class MoveableGeometry {
 public record Geometry(MoveablePackCpu.Data source,CollisionShape shape,float[] triangles,float[] rawToWorld,float yaw,float scale,int bytes){public Geometry{triangles=triangles.clone();rawToWorld=rawToWorld.clone();}public float[] triangles(){return triangles.clone();}public float[] rawToWorld(){return rawToWorld.clone();}}
 public static Geometry fit(PackBind binding,MoveablePackCpu.Data source,float yaw,float scale){
  if(source==null)throw new IllegalArgumentException("Moveable source");var fit=MoveableMesh.fit(binding,source.geometry(),yaw,scale);var positions=fit.shape().vertices();var raw=source.vertices();var faces=source.faces();if(faces.length>60000)throw new IllegalArgumentException("Moveable visual triangle budget");var output=new float[faces.length*8];int n=0;
  for(int index:faces){int at=index*8,point=index*3;output[n++]=-positions[point];output[n++]=positions[point+2];output[n++]=-positions[point+1];float nx=raw[at+3],ny=raw[at+4],nz=raw[at+5];output[n++]=nx*fit.binding().cos-nz*fit.binding().sin;output[n++]=ny;output[n++]=nx*fit.binding().sin+nz*fit.binding().cos;output[n++]=raw[at+6];output[n++]=raw[at+7];}
  var b=fit.binding();var rawToWorld=new Matrix4f(-b.cos*b.scale,-b.sin*b.scale,0,0,0,0,b.scale,0,b.sin*b.scale,-b.cos*b.scale,0,0,b.x,b.y,b.z,1).get(new float[16]);
  return new Geometry(source,fit.shape(),output,rawToWorld,yaw,scale,(output.length+positions.length+faces.length+16)*4);
 }
 private static final Matrix4f BASIS=new Matrix4f(-1,0,0,0,0,0,-1,0,0,1,0,0,0,0,0,1);
 public static Matrix4f world(float[] pose){if(pose==null||pose.length!=7)throw new IllegalArgumentException("Moveable body pose");for(float value:pose)if(!Float.isFinite(value))throw new IllegalArgumentException("Moveable body numeric pose");double norm=pose[3]*pose[3]+pose[4]*pose[4]+pose[5]*pose[5]+pose[6]*pose[6];if(Math.abs(norm-1)>.002)throw new IllegalArgumentException("Moveable body quaternion");return new Matrix4f().translation(pose[0],pose[1],pose[2]).rotate(new Quaternionf(pose[3],pose[4],pose[5],pose[6]).normalize());}
 public static float[] pose(Matrix4f matrix){if(matrix==null||!matrix.isFinite()||Math.abs(matrix.determinant()-1)>.002)throw new IllegalArgumentException("Moveable rigid matrix");var x=new Vector3f(matrix.m00(),matrix.m01(),matrix.m02());var y=new Vector3f(matrix.m10(),matrix.m11(),matrix.m12());var z=new Vector3f(matrix.m20(),matrix.m21(),matrix.m22());if(Math.abs(x.lengthSquared()-1)>.002||Math.abs(y.lengthSquared()-1)>.002||Math.abs(z.lengthSquared()-1)>.002||Math.abs(x.dot(y))>.002||Math.abs(x.dot(z))>.002||Math.abs(y.dot(z))>.002)throw new IllegalArgumentException("Moveable matrix scale/shear");var p=matrix.getTranslation(new Vector3f());var q=matrix.getNormalizedRotation(new Quaternionf()).normalize();return new float[]{p.x,p.y,p.z,q.x,q.y,q.z,q.w};}
 public static Matrix4f render(float[] pose,ContactFrame.Point camera){if(camera==null)throw new IllegalArgumentException("Moveable camera");return new Matrix4f(BASIS).mul(new Matrix4f().translation((float)-camera.x(),(float)-camera.y(),(float)-camera.height())).mul(world(pose)).mul(new Matrix4f(BASIS).transpose());}
 /** Render point in camera coordinates equals the same world vertex under the physics body transform. */
 public static Vector3f worldPoint(float[] pose,float x,float y,float h){return world(pose).transformPosition(new Vector3f(x,y,h));}
 /** Native drop resets WorldZRotation. Preserve the actual carried model pose under that new item-local basis. */
 public static float[] rebase(float[] oldPose,Geometry oldGeometry,Geometry nextGeometry){if(oldGeometry==null||nextGeometry==null||oldGeometry.source!=nextGeometry.source||oldGeometry.scale!=nextGeometry.scale)throw new IllegalArgumentException("Moveable drop basis changed");return pose(world(oldPose).mul(new Matrix4f().set(oldGeometry.rawToWorld)).mul(new Matrix4f().set(nextGeometry.rawToWorld).invert()));}
 private MoveableGeometry(){}
}
