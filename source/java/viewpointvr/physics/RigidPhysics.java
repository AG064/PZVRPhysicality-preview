package viewpointvr.physics;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Typed, owner-thread JNI boundary. It passes only bounded numeric arrays and opaque handles. */
public final class RigidPhysics implements AutoCloseable {
 private static final float[] EMPTY=new float[0];private static final int[] NO_FACES=new int[0];
 private static boolean loaded;
 private final Thread owner=Thread.currentThread();private long world;
 private static native long nCall(long world,int operation,long first,long second,float[] input,int[] faces,float[] output);
 public static synchronized void load(Path path)throws Exception {
  Path file=path.toRealPath();
  if(!Files.isRegularFile(file)||Files.size(file)>32_000_000)throw new IllegalArgumentException("Physics library budget");
  try(var stream=Files.newInputStream(file)){
   String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes()));
   if(!actual.equals(PhysicsLibraryPin.SHA256))throw new IllegalArgumentException("Physics library differs from tested solver");
  }
  if(!loaded){System.load(file.toString());loaded=true;}
 }
 public static synchronized void loadDefault()throws Exception {
  if(loaded)return;Path jar=Path.of(RigidPhysics.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
  if(!jar.getFileName().toString().endsWith(".jar"))throw new IllegalStateException("Native solver must accompany its tested JAR");
  load(jar.getParent().resolve("native/pzvr_physics.dll"));
 }
 public RigidPhysics(){if(!loaded)throw new IllegalStateException("Physics library not loaded");world=nCall(0,0,0,0,EMPTY,NO_FACES,EMPTY);if(world<=0)throw new IllegalStateException("Physics world unavailable");}
 private void check(){if(Thread.currentThread()!=owner)throw new IllegalStateException("Physics world thread changed");if(world==0)throw new IllegalStateException("Physics world closed");}
 private long call(int operation,long a,long b,float[] input,int[] faces,float[] output){check();return nCall(world,operation,a,b,input,faces,output);}
 private static float[] bodyInput(float mass,float[] pose,float[] geometry){if(pose.length!=7)throw new IllegalArgumentException("Physics pose");var input=new float[8+geometry.length];input[0]=mass;System.arraycopy(pose,0,input,1,7);System.arraycopy(geometry,0,input,8,geometry.length);return input;}
 public long boxes(int kind,float[] pose,float mass,float[] boxes){return call(2,kind,0,bodyInput(mass,pose,boxes),NO_FACES,EMPTY);}
 public long mesh(int kind,float[] pose,float mass,float[] vertices,int[] triangles){return call(3,kind,0,bodyInput(mass,pose,vertices),triangles,EMPTY);}
 public int prepareMesh(float[] vertices,int[] triangles){var output=new float[1];call(23,0,0,vertices,triangles,output);return (int)output[0];}
 public long preparedMesh(float[] pose,float mass,float[] vertices,int[] triangles){return call(24,0,0,bodyInput(mass,pose,vertices),triangles,EMPTY);}
 public void remove(long body){call(4,body,0,EMPTY,NO_FACES,EMPTY);}
 public void drive(long body,float[] pose){call(5,body,0,pose,NO_FACES,EMPTY);}
 public void groups(long body,int membership,int filter){call(15,body,0,EMPTY,new int[]{membership,filter},EMPTY);}
 public void velocity(long body,float[] values){call(6,body,0,values,NO_FACES,EMPTY);}
 public void sleep(long body){call(25,body,0,EMPTY,NO_FACES,EMPTY);}
 public void spring(long body,float[] pose,float[] tuning){if(pose.length!=7||tuning.length!=6)throw new IllegalArgumentException("Held target extent");var values=new float[13];System.arraycopy(pose,0,values,0,7);System.arraycopy(tuning,0,values,7,6);call(13,body,0,values,NO_FACES,EMPTY);}
 public void releaseSpring(long body){call(14,body,0,EMPTY,NO_FACES,EMPTY);}
 public long pin(long parent,long child,float[] relativePose){return call(7,parent,child,relativePose,NO_FACES,EMPTY);}
 public long hinge(long parent,long child,float[] anchorsAxisLimits){return call(8,parent,child,anchorsAxisLimits,NO_FACES,EMPTY);}
 public void motor(long hinge,float target,float stiffness,float damping,float maxForce){call(9,hinge,0,new float[]{target,stiffness,damping,maxForce},NO_FACES,EMPTY);}
 public float angle(long hinge){var output=new float[1];call(10,hinge,0,EMPTY,NO_FACES,output);return output[0];}
 public long slider(long parent,long child,float[] anchorsAxisLimits){return call(17,parent,child,anchorsAxisLimits,NO_FACES,EMPTY);}
 public void slideMotor(long slider,float target,float stiffness,float damping,float maxForce){call(18,slider,0,new float[]{target,stiffness,damping,maxForce},NO_FACES,EMPTY);}
 public float travel(long slider){var output=new float[1];call(19,slider,0,EMPTY,NO_FACES,output);return output[0];}
 public void gravityScale(long body,float scale){call(22,body,0,new float[]{scale},NO_FACES,EMPTY);}
 public void step(float seconds){call(11,0,0,new float[]{seconds},NO_FACES,EMPTY);}
 public float[] state(long body){var output=new float[14];call(12,body,0,EMPTY,NO_FACES,output);return output;}
 public float distance(long body,float[] point){if(point.length!=3)throw new IllegalArgumentException("Surface point extent");var output=new float[1];call(16,body,0,point,NO_FACES,output);return output[0];}
 public float[] ray(float[] origin,float[] direction,float range,long[] excluded){if(origin.length!=3||direction.length!=3||excluded.length>256)throw new IllegalArgumentException("Ray query extent");var input=new float[]{origin[0],origin[1],origin[2],direction[0],direction[1],direction[2],range};var ids=new int[excluded.length*2];for(int i=0;i<excluded.length;i++){ids[i*2]=(int)excluded[i];ids[i*2+1]=(int)(excluded[i]>>>32);}var output=new float[7];return call(20,0,0,input,ids,output)==0?null:output;}
 public float[] capsule(float[] origin,float[] direction,float radius,float height,float range,long[] excluded){if(origin.length!=3||direction.length!=3||excluded.length>256)throw new IllegalArgumentException("Capsule query extent");var input=new float[]{origin[0],origin[1],origin[2],direction[0],direction[1],direction[2],radius,height,range};var ids=new int[excluded.length*2];for(int i=0;i<excluded.length;i++){ids[i*2]=(int)excluded[i];ids[i*2+1]=(int)(excluded[i]>>>32);}var output=new float[4];return call(21,0,0,input,ids,output)==0?null:output;}
 public void close(){if(world==0)return;check();nCall(world,1,0,0,EMPTY,NO_FACES,EMPTY);world=0;}
 public static float[] pose(float x,float y,float z){return new float[]{x,y,z,0,0,0,1};}
 public static float[] box(float x,float y,float z,float cx,float cy,float cz){return new float[]{x,y,z,cx,cy,cz,0,0,0,1};}
}
