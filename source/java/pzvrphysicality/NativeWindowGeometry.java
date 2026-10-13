package pzvrphysicality;

import java.util.*;
import viewpointvr.physics.CollisionShape;

/** Numeric copies of the selected provider's triangles; no wall or pane bounding proxies. */
public final class NativeWindowGeometry {
 public static final int MAX_FLOATS=96000,MAX_APERTURE_FLOATS=12288;
 public record Data(boolean north,float plane,float[] mesh,float[] aperture,float[] bounds){
  public Data{
   validate(mesh,MAX_FLOATS);validate(aperture,MAX_APERTURE_FLOATS);
   if(bounds==null||bounds.length!=6||!Float.isFinite(plane)||Math.abs(plane)>4)throw new IllegalArgumentException("Window bounds/plane");
   bounds=bounds.clone();for(float value:bounds)if(!Float.isFinite(value)||Math.abs(value)>4)throw new IllegalArgumentException("Window bounds");
   for(int axis=0;axis<3;axis++)if(bounds[axis]>bounds[axis+3])throw new IllegalArgumentException("Window bounds order");
   if(bounds[3]-bounds[0]>4||bounds[4]-bounds[1]>4||bounds[5]-bounds[2]>4)throw new IllegalArgumentException("Window envelope budget");
   for(var values:List.of(mesh,aperture))for(int at=0;at<values.length;at+=8)for(int axis=0;axis<3;axis++)if(values[at+axis]<bounds[axis]-.03f||values[at+axis]>bounds[axis+3]+.03f)throw new IllegalArgumentException("Window mesh/envelope mismatch");
   planar(aperture,north,plane);mesh=mesh.clone();aperture=aperture.clone();
  }
  public float[] mesh(){return mesh.clone();}public float[] aperture(){return aperture.clone();}public float[] bounds(){return bounds.clone();}
  public boolean matches(boolean n,float p,float[] m,float[] a,float[] b){return north==n&&Float.floatToIntBits(plane)==Float.floatToIntBits(p)&&Arrays.equals(mesh,m)&&Arrays.equals(aperture,a)&&Arrays.equals(bounds,b);}
 }
 private static void validate(float[] values,int budget){
  if(values==null||values.length>budget||values.length%24!=0)throw new IllegalArgumentException("Window triangle-list budget");
  for(int at=0;at<values.length;at+=8)for(int axis=0;axis<8;axis++)if(!Float.isFinite(values[at+axis])||axis<3&&Math.abs(values[at+axis])>4)throw new IllegalArgumentException("Window numeric triangle data");
 }
 private static void planar(float[] values,boolean north,float plane){for(int at=0;at<values.length;at+=8)if(Math.abs(values[at+(north?2:0)]-plane)>.0001f)throw new IllegalArgumentException("Nonplanar native aperture");}
 private static double cross(double ax,double ay,double bx,double by){return ax*by-ay*bx;}
 private static boolean inside(float[] aperture,boolean north,double x,double y){
  int axis=north?0:2;
  for(int at=0;at<aperture.length;at+=24){double ax=aperture[at+axis],ay=aperture[at+1],bx=aperture[at+8+axis],by=aperture[at+9],cx=aperture[at+16+axis],cy=aperture[at+17];
   double area=cross(bx-ax,by-ay,cx-ax,cy-ay);if(Math.abs(area)<1e-12)continue;
   double u=cross(bx-x,by-y,cx-x,cy-y)/area,v=cross(cx-x,cy-y,ax-x,ay-y)/area,w=1-u-v;
   if(u>=-1e-6&&v>=-1e-6&&w>=-1e-6)return true;
  }return false;
 }
 private static boolean pane(Data data,int at){
  if(data.aperture.length==0)return false;int depth=data.north?2:0,axis=data.north?0:2;
  for(int v=0;v<3;v++)if(Math.abs(data.mesh[at+v*8+depth]-data.plane)>.0001f)return false;
  double x=0,y=0;
  for(int v=0;v<3;v++){int a=at+v*8,b=at+(v+1)%3*8;double px=data.mesh[a+axis],py=data.mesh[a+1];x+=px;y+=py;if(!inside(data.aperture,data.north,px,py)||!inside(data.aperture,data.north,(px+data.mesh[b+axis])*.5,(py+data.mesh[b+1])*.5))return false;}
  return inside(data.aperture,data.north,x/3,y/3);
 }
 private static final class Builder{
  final List<Float> vertices=new ArrayList<>();final List<Integer> faces=new ArrayList<>();
  void add(float[] values,int at){
   double ax=values[at+8]-values[at],ay=values[at+9]-values[at+1],az=values[at+10]-values[at+2],bx=values[at+16]-values[at],by=values[at+17]-values[at+1],bz=values[at+18]-values[at+2];
   double x=ay*bz-az*by,y=az*bx-ax*bz,z=ax*by-ay*bx;if(x*x+y*y+z*z<1e-18)return;
   if(vertices.size()+9>96000)throw new IllegalArgumentException("Window contact vertex budget");int first=vertices.size()/3;
   for(int v=0;v<3;v++){int offset=at+v*8;vertices.add(values[offset]);vertices.add(values[offset+2]);vertices.add(values[offset+1]);}
   faces.add(first);faces.add(first+2);faces.add(first+1);
  }
  CollisionShape finish(){if(vertices.isEmpty())return null;float[] p=new float[vertices.size()];int[] f=new int[faces.size()];for(int i=0;i<p.length;i++)p[i]=vertices.get(i);for(int i=0;i<f.length;i++)f[i]=faces.get(i);return new CollisionShape(p,f,new float[0],true,true);}
 }
 public static CollisionShape read(float[] mesh){validate(mesh,MAX_FLOATS);var out=new Builder();for(int at=0;at<mesh.length;at+=24)out.add(mesh,at);return out.finish();}
 public static CollisionShape glass(float[] aperture,boolean north,float plane){validate(aperture,MAX_APERTURE_FLOATS);if(!Float.isFinite(plane)||Math.abs(plane)>4)throw new IllegalArgumentException("Window glass plane");planar(aperture,north,plane);var out=new Builder();for(int at=0;at<aperture.length;at+=24)out.add(aperture,at);return out.finish();}
 public static CollisionShape window(Data data,boolean glass){
  Objects.requireNonNull(data);var out=new Builder();for(int at=0;at<data.mesh.length;at+=24)if(!pane(data,at))out.add(data.mesh,at);
  if(glass)for(int at=0;at<data.aperture.length;at+=24)out.add(data.aperture,at);return out.finish();
 }
 /** Two-sided exact segment test. An intersection only at either endpoint is excluded. */
 public static boolean intersects(CollisionShape shape,double fx,double fy,double fz,double tx,double ty,double tz){
  for(double value:new double[]{fx,fy,fz,tx,ty,tz})if(!Double.isFinite(value)||Math.abs(value)>16)throw new IllegalArgumentException("Window segment bounds");
  if(shape==null)return false;var vertices=shape.vertices();var faces=shape.faces();if(vertices.length>96000||faces.length>192000||faces.length%3!=0||shape.boxes().length!=0)throw new IllegalArgumentException("Window segment mesh");
  double dx=tx-fx,dy=ty-fy,dz=tz-fz;if(dx*dx+dy*dy+dz*dz<1e-16)return false;
  for(int at=0;at<faces.length;at+=3){int a=faces[at]*3,b=faces[at+1]*3,c=faces[at+2]*3;if(a<0||b<0||c<0||a>vertices.length-3||b>vertices.length-3||c>vertices.length-3)throw new IllegalArgumentException("Window segment index");
   double ux=vertices[b]-vertices[a],uy=vertices[b+1]-vertices[a+1],uz=vertices[b+2]-vertices[a+2],vx=vertices[c]-vertices[a],vy=vertices[c+1]-vertices[a+1],vz=vertices[c+2]-vertices[a+2];
   double px=dy*vz-dz*vy,py=dz*vx-dx*vz,pz=dx*vy-dy*vx,det=ux*px+uy*py+uz*pz;if(Math.abs(det)<1e-12)continue;
   double sx=fx-vertices[a],sy=fy-vertices[a+1],sz=fz-vertices[a+2],u=(sx*px+sy*py+sz*pz)/det;if(u< -1e-8||u>1+1e-8)continue;
   double qx=sy*uz-sz*uy,qy=sz*ux-sx*uz,qz=sx*uy-sy*ux,v=(dx*qx+dy*qy+dz*qz)/det;if(v< -1e-8||u+v>1+1e-8)continue;
   double t=(vx*qx+vy*qy+vz*qz)/det;if(t>1e-6&&t<1-1e-6)return true;
  }return false;
 }
 private NativeWindowGeometry(){}
}
