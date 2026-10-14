package pzvrphysicality;
import java.util.*;import org.joml.Matrix4f;import org.joml.Vector3f;
/** Native socket orientation/translation plus an already-scaled original world drawer. No authored grip offset. */
public final class NativeHeldMount {
 /** Exact Viewpoint RigidCapture.placed basis and InventoryItem world rotations. */
 public static Matrix4f world(float x,float y,float z,float camX,float camY,float camZ,float rx,float ry,float rz){
  float[] values={x,y,z,camX,camY,camZ,rx,ry,rz};for(float value:values)if(!Float.isFinite(value))throw new IllegalArgumentException("Native world placement numeric");
  return new Matrix4f().translation(-(x-camX),(z-camZ)*2.4494896f,-(y-camY)).scale(-1.5f,1.5f,1.5f).rotateX((float)Math.toRadians(rx)).rotateY((float)Math.toRadians(rz)).rotateZ((float)Math.toRadians(ry));
 }
 public static Matrix4f correction(float[] actor,float[] prop,float[] world){
  if(world==null||world.length!=16)throw new IllegalArgumentException("Native world placement extent");var source=new Matrix4f().set(world);if(!source.isFinite()||Math.abs(source.m03())+Math.abs(source.m13())+Math.abs(source.m23())+Math.abs(source.m33()-1)>.0001f||Math.abs(source.determinant()+3.375f)>.01f)throw new IllegalArgumentException("Native world placement basis");
  // Original world rows already own the FBX/mesh/script transform. Replace only their proven Viewpoint placement.
  return socket(actor,prop).scale(1.5f).mul(source.invert());
 }
 public static Matrix4f socket(float[] actor,float[] prop){
  if(actor==null||prop==null||actor.length!=16||prop.length!=16)throw new IllegalArgumentException("Native socket extent");
  var value=new Matrix4f().set(actor).mul(new Matrix4f().set(prop));
  if(!value.isFinite()||Math.abs(value.m03())+Math.abs(value.m13())+Math.abs(value.m23())+Math.abs(value.m33()-1)>.0001f)throw new IllegalArgumentException("Native socket affine");
  var x=value.getColumn(0,new org.joml.Vector4f());var y=value.getColumn(1,new org.joml.Vector4f());var z=value.getColumn(2,new org.joml.Vector4f());
  float a=(float)Math.sqrt(x.x*x.x+x.y*x.y+x.z*x.z),b=(float)Math.sqrt(y.x*y.x+y.y*y.y+y.z*y.z),c=(float)Math.sqrt(z.x*z.x+z.y*z.y+z.z*z.z);
  if(!Float.isFinite(a)||a<1e-7f||a>1000||Math.abs(a-b)>a*.002f||Math.abs(a-c)>a*.002f||Math.abs(x.x*y.x+x.y*y.y+x.z*y.z)>a*a*.002f||Math.abs(x.x*z.x+x.y*z.y+x.z*z.z)>a*a*.002f||Math.abs(y.x*z.x+y.y*z.y+y.z*z.z)>a*a*.002f)throw new IllegalArgumentException("Native socket nonrigid basis");
  // Strip skeletal unit scale only. The original world model already owns mesh/script scale and reflection.
  value.m00(value.m00()/a).m01(value.m01()/a).m02(value.m02()/a).m10(value.m10()/a).m11(value.m11()/a).m12(value.m12()/a).m20(value.m20()/a).m21(value.m21()/a).m22(value.m22()/a);
  if(Math.abs(Math.abs(value.determinant())-1)>.004f)throw new IllegalArgumentException("Native socket basis determinant");return value;
 }
 public static int apply(float[] values,int[] flags,int[] palette,int[] shadow,int first,int last,Matrix4f socket){
  if(values==null||flags==null||palette==null||shadow==null||first<0||last<=first||last-first>4||last>values.length/68||last>flags.length||last>palette.length||last>shadow.length||socket==null||!socket.isFinite()||Math.abs(Math.abs(socket.determinant())-1)>.004f)throw new IllegalArgumentException("Native held packet");
  var copied=new ArrayList<float[]>();
  for(int row=first;row<last;row++){
   if(palette[row]>=0||shadow[row]>=0)throw new IllegalArgumentException("World fallback must be rigid");var data=Arrays.copyOfRange(values,row*68,row*68+68);var original=new Matrix4f().set(data);
   for(float value:data)if(!Float.isFinite(value))throw new IllegalArgumentException("Native drawer numeric payload");if(data[27]<0||!original.isFinite()||Math.abs(original.determinant())<1e-18f)throw new IllegalArgumentException("Native drawer affine");
   var changed=new Matrix4f(socket).mul(original);var centre=socket.transformPosition(new Vector3f(data[24],data[25],data[26]));if(!changed.isFinite()||!centre.isFinite())throw new IllegalArgumentException("Native held transform overflow");
   changed.get(data,0);changed.get(data,36);data[24]=centre.x;data[25]=centre.y;data[26]=centre.z;copied.add(data);
  }
  for(int row=first;row<last;row++){System.arraycopy(copied.get(row-first),0,values,row*68,68);flags[row]&=~8;}return copied.size();
 }
 private NativeHeldMount(){}
}
