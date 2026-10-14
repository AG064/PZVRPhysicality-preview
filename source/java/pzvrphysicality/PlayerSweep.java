package pzvrphysicality;

import java.util.*;
import viewpointvr.physics.CollisionShape;

/** Horizontal avatar sweep against actual triangles clipped to the occupied body height. */
public final class PlayerSweep {
 public static final int MAX_TRIANGLES=12000,MAX_SHAPES=128;
 private static final double EPS=1e-8,SKIN=.0005;
 public record Triangle(double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz){}
 public record Mesh(List<Triangle> triangles,double minX,double maxX,double minY,double maxY,double minZ,double maxZ){public Mesh{triangles=List.copyOf(triangles);}public double planeDepth(){
  for(var t:triangles){double nx=(t.by-t.ay)*(t.cz-t.az)-(t.bz-t.az)*(t.cy-t.ay),ny=(t.bz-t.az)*(t.cx-t.ax)-(t.bx-t.ax)*(t.cz-t.az),nz=(t.bx-t.ax)*(t.cy-t.ay)-(t.by-t.ay)*(t.cx-t.ax),length=Math.sqrt(nx*nx+ny*ny+nz*nz);if(length<=EPS)continue;nx/=length;ny/=length;nz/=length;double low=0,high=0;for(var p:triangles)for(double[] v:new double[][]{{p.ax,p.ay,p.az},{p.bx,p.by,p.bz},{p.cx,p.cy,p.cz}}){double d=nx*(v[0]-t.ax)+ny*(v[1]-t.ay)+nz*(v[2]-t.az);low=Math.min(low,d);high=Math.max(high,d);}return high-low;}return 0;
 }}
 public record Result(double x,double y,int contacts,int triangles,boolean budget){public boolean clipped(double dx,double dy){return Math.hypot(x-dx,y-dy)>1e-6;}}
 private record Hit(double time,double x,double y){}
 public static Mesh read(CollisionShape shape,float[] pose){
  if(shape==null||pose==null||pose.length!=7)throw new IllegalArgumentException("Player mesh pose");
  for(float n:pose)if(!Float.isFinite(n))throw new IllegalArgumentException("Player mesh pose");
  double q=pose[3]*pose[3]+pose[4]*pose[4]+pose[5]*pose[5]+pose[6]*pose[6];if(Math.abs(q-1)>.002)throw new IllegalArgumentException("Player mesh rotation");
  float[] vertices=shape.vertices();int[] faces=shape.faces();if(vertices.length<9||faces.length<3||vertices.length%3!=0||faces.length%3!=0||vertices.length>96000||faces.length>180000)throw new IllegalArgumentException("Player requires actual triangle mesh");
  double[] world=new double[vertices.length];double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=-minX,maxY=maxX,maxZ=maxX;
  for(int i=0;i<vertices.length;i+=3){double x=vertices[i],y=vertices[i+1],z=vertices[i+2];if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Player mesh vertex");
   double tx=2*(pose[4]*z-pose[5]*y),ty=2*(pose[5]*x-pose[3]*z),tz=2*(pose[3]*y-pose[4]*x);
   x+=pose[6]*tx+pose[4]*tz-pose[5]*ty;y+=pose[6]*ty+pose[5]*tx-pose[3]*tz;z+=pose[6]*tz+pose[3]*ty-pose[4]*tx;
   x+=pose[0];y+=pose[1];z+=pose[2];world[i]=x;world[i+1]=y;world[i+2]=z;
   minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
  }
  var out=new ArrayList<Triangle>();for(int i=0;i<faces.length;i+=3){int a=faces[i]*3,b=faces[i+1]*3,c=faces[i+2]*3;if(a<0||b<0||c<0||a+2>=world.length||b+2>=world.length||c+2>=world.length)throw new IllegalArgumentException("Player mesh index");out.add(new Triangle(world[a],world[a+1],world[a+2],world[b],world[b+1],world[b+2],world[c],world[c+1],world[c+2]));}
  return new Mesh(out,minX,maxX,minY,maxY,minZ,maxZ);
 }
 private static List<double[]> clip(List<double[]> values,double height,boolean above){
  var result=new ArrayList<double[]>();if(values.isEmpty())return result;double[] before=values.getLast();boolean old=above?before[2]>=height:before[2]<=height;
  for(var point:values){boolean current=above?point[2]>=height:point[2]<=height;if(old!=current){double t=(height-before[2])/(point[2]-before[2]);result.add(new double[]{before[0]+t*(point[0]-before[0]),before[1]+t*(point[1]-before[1]),height});}if(current)result.add(point);before=point;old=current;}return result;
 }
 private static List<double[]> polygon(Triangle t,double low,double high){
  var p=clip(clip(List.of(new double[]{t.ax,t.ay,t.az},new double[]{t.bx,t.by,t.bz},new double[]{t.cx,t.cy,t.cz}),low,true),high,false);var unique=new ArrayList<double[]>();
  for(var v:p)if(unique.isEmpty()||Math.hypot(v[0]-unique.getLast()[0],v[1]-unique.getLast()[1])>EPS)unique.add(v);
  if(unique.size()>1&&Math.hypot(unique.getFirst()[0]-unique.getLast()[0],unique.getFirst()[1]-unique.getLast()[1])<=EPS)unique.removeLast();return unique;
 }
 private static boolean inside(List<double[]> p,double x,double y){if(p.size()<3)return false;double sign=0,area=0;for(int i=0;i<p.size();i++){var a=p.get(i);var b=p.get((i+1)%p.size());area+=a[0]*b[1]-a[1]*b[0];double c=(b[0]-a[0])*(y-a[1])-(b[1]-a[1])*(x-a[0]);if(Math.abs(c)>EPS){if(sign!=0&&sign*c<0)return false;sign=c;}}return Math.abs(area)>EPS;}
 private static boolean coincident(List<double[]> p,double x,double y){if(p.isEmpty()||inside(p,x,y))return inside(p,x,y);double area=0;for(int i=0;i<p.size();i++){var a=p.get(i);var b=p.get((i+1)%p.size());area+=a[0]*b[1]-a[1]*b[0];}if(Math.abs(area)>EPS)return false;for(int i=0;i<p.size();i++){var a=p.get(i);var b=p.get((i+1)%p.size());double dx=b[0]-a[0],dy=b[1]-a[1],n=dx*dx+dy*dy,u=n<EPS?0:Math.clamp(((x-a[0])*dx+(y-a[1])*dy)/n,0,1);if(Math.hypot(x-a[0]-dx*u,y-a[1]-dy*u)<EPS)return true;}return false;}
 private static Hit point(double px,double py,double dx,double dy,double x,double y,double radius){double ox=px-x,oy=py-y,a=dx*dx+dy*dy,b=ox*dx+oy*dy,c=ox*ox+oy*oy-radius*radius;if(a<EPS)return null;
  if(c<=0){double n=Math.hypot(ox,oy);return n>EPS&&b<0?new Hit(0,ox/n,oy/n):null;}double disc=b*b-a*c;if(disc<0||b>=0)return null;double time=(-b-Math.sqrt(disc))/a;if(time<0||time>1)return null;double nx=ox+dx*time,ny=oy+dy*time,n=Math.hypot(nx,ny);return n>EPS?new Hit(time,nx/n,ny/n):null;}
 private static Hit segment(double px,double py,double dx,double dy,double[] a,double[] b,double radius){
  double ex=b[0]-a[0],ey=b[1]-a[1],length=Math.hypot(ex,ey);Hit best=point(px,py,dx,dy,a[0],a[1],radius);var end=point(px,py,dx,dy,b[0],b[1],radius);if(end!=null&&(best==null||end.time<best.time))best=end;if(length<EPS)return best;
  double nx=-ey/length,ny=ex/length,distance=(px-a[0])*nx+(py-a[1])*ny,speed=dx*nx+dy*ny;
  for(int side:new int[]{-1,1}){double nX=nx*side,nY=ny*side,d=distance*side,v=speed*side;if(v>=-EPS||d<-EPS)continue;double time=d<=radius?0:(radius-d)/v;if(time<0||time>1)continue;double atX=px+dx*time,atY=py+dy*time,u=((atX-a[0])*ex+(atY-a[1])*ey)/(length*length);if(u>=0&&u<=1&&(best==null||time<best.time))best=new Hit(time,nX,nY);}
  return best;
 }
 public static Result move(List<Mesh> meshes,double x,double y,double feet,double height,double radius,double dx,double dy){
  for(double value:new double[]{x,y,feet,height,radius,dx,dy})if(!Double.isFinite(value))throw new IllegalArgumentException("Player sweep number");
  if(height<.4||height>3||radius<.05||radius>.6||meshes.size()>MAX_SHAPES)throw new IllegalArgumentException("Player capsule extent");
  double px=x,py=y,vx=dx,vy=dy;int contacts=0,queries=0;var overlaps=new IdentityHashMap<Mesh,Boolean>();
  for(int pass=0;pass<3&&Math.hypot(vx,vy)>EPS;pass++){
   Hit hit=null;for(var mesh:meshes){if(mesh.maxZ<feet+.15||mesh.minZ>feet+height||Math.max(px,px+vx)+radius<mesh.minX||Math.min(px,px+vx)-radius>mesh.maxX||Math.max(py,py+vy)+radius<mesh.minY||Math.min(py,py+vy)-radius>mesh.maxY)continue;
    if(!overlaps.containsKey(mesh)){boolean contained=false;for(var triangle:mesh.triangles){if(++queries>MAX_TRIANGLES)return new Result(dx,dy,0,queries,true);if(coincident(polygon(triangle,feet+.15,feet+height),x,y)){contained=true;break;}}overlaps.put(mesh,contained);}if(overlaps.get(mesh))continue;
    for(var triangle:mesh.triangles){if(++queries>MAX_TRIANGLES)return new Result(dx,dy,0,queries,true);var polygon=polygon(triangle,feet+.15,feet+height);if(polygon.isEmpty()||inside(polygon,px,py))continue;for(int edge=0;edge<polygon.size();edge++){var candidate=segment(px,py,vx,vy,polygon.get(edge),polygon.get((edge+1)%polygon.size()),radius);if(candidate!=null&&(hit==null||candidate.time<hit.time))hit=candidate;}}
   }
   if(hit==null){px+=vx;py+=vy;break;}double travel=Math.max(0,hit.time-SKIN/Math.max(SKIN,Math.hypot(vx,vy)));px+=vx*travel;py+=vy*travel;vx*=1-travel;vy*=1-travel;double into=vx*hit.x+vy*hit.y;if(into<0){vx-=into*hit.x;vy-=into*hit.y;}contacts++;
  }
  return new Result(px-x,py-y,contacts,queries,false);
 }
 /** A refinement requires a clear initial capsule too; overlap escape is not permission to bypass a native tile. */
 public static boolean clearPath(Mesh mesh,double x,double y,double feet,double height,double radius,double dx,double dy){
  for(double value:new double[]{x,y,feet,height,radius,dx,dy})if(!Double.isFinite(value))return false;
  if(mesh==null||mesh.triangles.size()>2000||height<.4||height>3||radius<.05||radius>.6)return false;
  for(var triangle:mesh.triangles){var p=polygon(triangle,feet+.15,feet+height);if(inside(p,x,y))return false;
   for(int i=0;i<p.size();i++){var a=p.get(i);var b=p.get((i+1)%p.size());double ex=b[0]-a[0],ey=b[1]-a[1],n=ex*ex+ey*ey,u=n<EPS?0:Math.clamp(((x-a[0])*ex+(y-a[1])*ey)/n,0,1);
    if(Math.hypot(x-a[0]-u*ex,y-a[1]-u*ey)<=radius+SKIN)return false;}
  }
  var result=move(List.of(mesh),x,y,feet,height,radius,dx,dy);return !result.budget()&&!result.clipped(dx,dy);
 }
 private PlayerSweep(){}
}
