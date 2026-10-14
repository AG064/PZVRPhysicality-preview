package pzvrphysicality;
import java.util.*;import viewpointvr.physics.CollisionShape;

/** Independently authored tap only. It contains no original counter mesh, UV or texture payload. */
public final class FaucetMesh {
 public record Data(float[] vertices,int[] faces){public Data{vertices=vertices.clone();faces=faces.clone();if(vertices.length>12000||faces.length>24000)throw new IllegalArgumentException("Faucet budget");}public float[] vertices(){return vertices.clone();}public int[] faces(){return faces.clone();}}
 private static final Data CLOSED=build(false),OPEN=build(true);
 public static Data data(boolean open){return open?OPEN:CLOSED;}
 private static Data build(boolean open){var points=new ArrayList<MouthMath.Vec>();var faces=new ArrayList<Integer>();
  tube(points,faces,List.of(new MouthMath.Vec(0,.91,-.43),new MouthMath.Vec(0,.935,-.43)),.035,12);
  var arch=new ArrayList<MouthMath.Vec>();arch.add(new MouthMath.Vec(0,.925,-.43));arch.add(new MouthMath.Vec(0,1.16,-.43));
  // Smoothly bend towards the measured basin centre, then turn the outlet down.
  for(int n=1;n<=8;n++){double t=n*Math.PI/8;arch.add(new MouthMath.Vec(0,1.16+.065*Math.sin(t),-.43+.12125*(1-Math.cos(t))));}
  arch.add(new MouthMath.Vec(0,1.12,-.1875));tube(points,faces,arch,.013,10);
  for(double x:new double[]{-.115,.115}){
   tube(points,faces,List.of(new MouthMath.Vec(x,.91,-.43),new MouthMath.Vec(x,.965,-.43)),.023,10);
   double angle=open?Math.PI/4:0;for(int direction=0;direction<2;direction++){double a=angle+direction*Math.PI/2,dx=.041*Math.cos(a),dz=.041*Math.sin(a);
    tube(points,faces,List.of(new MouthMath.Vec(x-dx,.965,-.43-dz),new MouthMath.Vec(x+dx,.965,-.43+dz)),.007,8);}
  }
  var v=new float[points.size()*3];for(int i=0;i<points.size();i++){var p=points.get(i);v[i*3]=(float)p.x();v[i*3+1]=(float)p.y();v[i*3+2]=(float)p.z();}return new Data(v,faces.stream().mapToInt(Integer::intValue).toArray());
 }
 private static MouthMath.Vec sub(MouthMath.Vec a,MouthMath.Vec b){return new MouthMath.Vec(a.x()-b.x(),a.y()-b.y(),a.z()-b.z());}
 private static MouthMath.Vec cross(MouthMath.Vec a,MouthMath.Vec b){return new MouthMath.Vec(a.y()*b.z()-a.z()*b.y(),a.z()*b.x()-a.x()*b.z(),a.x()*b.y()-a.y()*b.x());}
 private static MouthMath.Vec unit(MouthMath.Vec p){double length=Math.sqrt(p.x()*p.x()+p.y()*p.y()+p.z()*p.z());if(length<1e-9)throw new IllegalArgumentException("Faucet path");return new MouthMath.Vec(p.x()/length,p.y()/length,p.z()/length);}
 private static void triangle(List<Integer> faces,int a,int b,int c){faces.add(a);faces.add(b);faces.add(c);}
 private static void tube(List<MouthMath.Vec> vertices,List<Integer> faces,List<MouthMath.Vec> path,double radius,int sides){int first=vertices.size();
  for(int ring=0;ring<path.size();ring++){var centre=path.get(ring);var tangent=unit(sub(path.get(Math.min(path.size()-1,ring+1)),path.get(Math.max(0,ring-1))));var axis=Math.abs(tangent.x())<.8?new MouthMath.Vec(1,0,0):new MouthMath.Vec(0,0,1);var u=unit(cross(tangent,axis));var v=cross(tangent,u);
   for(int n=0;n<sides;n++){double a=n*2*Math.PI/sides,c=Math.cos(a)*radius,s=Math.sin(a)*radius;vertices.add(new MouthMath.Vec(centre.x()+u.x()*c+v.x()*s,centre.y()+u.y()*c+v.y()*s,centre.z()+u.z()*c+v.z()*s));}
  }
  for(int ring=0;ring<path.size()-1;ring++)for(int n=0;n<sides;n++){int a=first+ring*sides+n,b=first+ring*sides+(n+1)%sides,c=a+sides,d=b+sides;triangle(faces,a,c,b);triangle(faces,b,c,d);}
  int low=vertices.size();vertices.add(path.getFirst());int high=vertices.size();vertices.add(path.getLast());for(int n=0;n<sides;n++){triangle(faces,low,first+(n+1)%sides,first+n);int top=first+(path.size()-1)*sides;triangle(faces,high,top+n,top+(n+1)%sides);}
 }
 public static CollisionShape shape(TapProfile profile,boolean open,float cos,float sin,float scale){if(profile==null||!profile.authored()||!Float.isFinite(cos)||!Float.isFinite(sin)||Math.abs(cos*cos+sin*sin-1)>.002||!Float.isFinite(scale)||scale<=0||scale>4)throw new IllegalArgumentException("Faucet placement");var d=data(open);var v=d.vertices();for(int i=0;i<v.length;i+=3){float x=v[i]*scale,y=v[i+1]*scale,z=v[i+2]*scale;v[i]=-x*cos+z*sin;v[i+1]=-x*sin-z*cos;v[i+2]=y;}var f=d.faces();for(int i=0;i<f.length;i+=3){int n=f[i+1];f[i+1]=f[i+2];f[i+2]=n;}return new CollisionShape(v,f,new float[0],true,true);}
 private FaucetMesh(){}
}
