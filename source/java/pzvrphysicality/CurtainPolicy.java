package pzvrphysicality;

import java.util.*;

/** Cloth contact and one lateral pull intent. Native actions own every state change. */
public final class CurtainPolicy<T> {
 public record Point(double x,double y,double z){public Point{if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Curtain point");}public double distance(Point other){return Math.sqrt((x-other.x)*(x-other.x)+(y-other.y)*(y-other.y)+(z-other.z)*(z-other.z));}}
 public record Intent<T>(T owner,Object geometry,boolean beforeOpen,boolean desiredOpen){}
 private static final class Hold<T>{final T owner;final Object geometry;final boolean open;final Point start;final boolean north;final double units;final long time;boolean fired;Hold(T owner,Object geometry,boolean open,Point start,boolean north,double units,long time){this.owner=owner;this.geometry=geometry;this.open=open;this.start=start;this.north=north;this.units=units;this.time=time;}}
 private final List<Hold<T>> held=new ArrayList<>(Arrays.asList(null,null));private final boolean[] neutral=new boolean[2];
 public void reset(){held.set(0,null);held.set(1,null);Arrays.fill(neutral,false);}
 public void release(int side){checkSide(side);held.set(side,null);neutral[side]=false;}
 public boolean holds(int side){return side>=0&&side<2&&held.get(side)!=null;}
 public T owner(int side){return holds(side)?held.get(side).owner:null;}
 public Intent<T> sample(int side,T owner,Object geometry,boolean open,boolean north,Point point,double units,boolean contact,boolean grip,boolean eligible,long now){
  checkSide(side);if(!eligible||point==null||!Double.isFinite(units)||units<.25||units>4){release(side);return null;}
  if(!grip){held.set(side,null);neutral[side]=true;return null;}
  var hold=held.get(side);
  if(hold==null){if(!neutral[side])return null;neutral[side]=false;if(owner==null||geometry==null||!contact)return null;held.set(side,new Hold<>(owner,geometry,open,point,north,units,now));return null;}
  if(owner!=hold.owner||geometry!=hold.geometry||open!=hold.open||north!=hold.north||Math.abs(units-hold.units)>.0001||now<hold.time||now-hold.time>1_500_000_000L){release(side);return null;}
  double along=north?point.x-hold.start.x:point.y-hold.start.y,depth=north?point.y-hold.start.y:point.x-hold.start.x,height=point.z-hold.start.z;
  if(Math.abs(depth)>.08*units||Math.abs(height)>.10*units||point.distance(hold.start)>.30*units){release(side);return null;}
  if(!hold.fired&&Math.abs(along)>=.10*units){hold.fired=true;return new Intent<>(hold.owner,hold.geometry,hold.open,!hold.open);}return null;
 }
 private static void checkSide(int side){if(side<0||side>1)throw new IllegalArgumentException("Curtain side");}
 public record Panel(float[] vertices,int[] faces,boolean north,double[] bounds){
  public Panel{if(vertices==null||faces==null||bounds==null||vertices.length<9||vertices.length%3!=0||vertices.length>36000||faces.length<3||faces.length%3!=0||faces.length>12000||bounds.length!=6)throw new IllegalArgumentException("Curtain panel extent");vertices=vertices.clone();faces=faces.clone();bounds=bounds.clone();for(float value:vertices)if(!Float.isFinite(value)||Math.abs(value)>4)throw new IllegalArgumentException("Curtain vertex");for(int index:faces)if(index<0||index>=vertices.length/3)throw new IllegalArgumentException("Curtain index");for(double value:bounds)if(!Double.isFinite(value)||Math.abs(value)>4)throw new IllegalArgumentException("Curtain bounds");for(int axis=0;axis<3;axis++)if(bounds[axis]>bounds[axis+3])throw new IllegalArgumentException("Curtain bounds order");}
  public float[] vertices(){return vertices.clone();}public int[] faces(){return faces.clone();}public double[] bounds(){return bounds.clone();}
 }
 private record P(double x,double y){P{if(x==0)x=0;if(y==0)y=0;}}
 private record Edge(P a,P b){static Edge of(P a,P b){int order=Double.compare(a.x,b.x);if(order==0)order=Double.compare(a.y,b.y);return order<0?new Edge(a,b):new Edge(b,a);}}
 /** Vertical silhouette edges are derived from the actual opaque front faces, including folded states. */
 public static boolean edge(Panel panel,Point point,double units){
  if(panel==null||point==null||units<.25||units>4)return false;double low=panel.bounds[2],high=panel.bounds[5];if(high-low<.2||point.z<low+(high-low)*.2||point.z>high-(high-low)*.15)return false;
  var edges=new HashMap<Edge,Integer>();for(int sign:new int[]{1,-1}){
   edges.clear();for(int at=0;at<panel.faces.length;at+=3){int a=panel.faces[at]*3,b=panel.faces[at+1]*3,c=panel.faces[at+2]*3;double ux=panel.vertices[b]-panel.vertices[a],uy=panel.vertices[b+1]-panel.vertices[a+1],uz=panel.vertices[b+2]-panel.vertices[a+2],vx=panel.vertices[c]-panel.vertices[a],vy=panel.vertices[c+1]-panel.vertices[a+1],vz=panel.vertices[c+2]-panel.vertices[a+2];double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,length=Math.sqrt(nx*nx+ny*ny+nz*nz);if(length<1e-12||(panel.north?ny:nx)*sign<length*.75)continue;
    int[] indices={a,b,c};for(int n=0;n<3;n++){int f=indices[n],t=indices[(n+1)%3];var first=new P(panel.vertices[f+(panel.north?0:1)],panel.vertices[f+2]);var next=new P(panel.vertices[t+(panel.north?0:1)],panel.vertices[t+2]);if(!first.equals(next))edges.merge(Edge.of(first,next),1,Integer::sum);}
   }
   if(!edges.isEmpty())break;
  }
  double along=panel.north?point.x:point.y;for(var item:edges.entrySet())if(item.getValue()==1){var edge=item.getKey();double dx=edge.b.x-edge.a.x,dy=edge.b.y-edge.a.y;if(Math.abs(dy)<.03||Math.abs(dy)<Math.abs(dx)*2)continue;double length=dx*dx+dy*dy,t=Math.max(0,Math.min(1,((along-edge.a.x)*dx+(point.z-edge.a.y)*dy)/length));if(Math.hypot(along-edge.a.x-t*dx,point.z-edge.a.y-t*dy)<=.06*units)return true;}return false;
 }
 public static Point closest(Panel panel,Point p){if(panel==null||p==null)return null;Point best=null;double distance=Double.POSITIVE_INFINITY;for(int n=0;n<panel.faces.length;n+=3){Point a=vertex(panel,panel.faces[n]),b=vertex(panel,panel.faces[n+1]),c=vertex(panel,panel.faces[n+2]);var q=closest(p,a,b,c);double d=p.distance(q);if(d<distance){best=q;distance=d;}}return best;}
 private static Point vertex(Panel panel,int index){int at=index*3;return new Point(panel.vertices[at],panel.vertices[at+1],panel.vertices[at+2]);}
 private static Point sub(Point a,Point b){return new Point(a.x-b.x,a.y-b.y,a.z-b.z);}private static double dot(Point a,Point b){return a.x*b.x+a.y*b.y+a.z*b.z;}private static Point add(Point a,Point b,double t){return new Point(a.x+b.x*t,a.y+b.y*t,a.z+b.z*t);}
 private static Point closest(Point p,Point a,Point b,Point c){var ab=sub(b,a);var ac=sub(c,a);var ap=sub(p,a);double d1=dot(ab,ap),d2=dot(ac,ap);if(d1<=0&&d2<=0)return a;var bp=sub(p,b);double d3=dot(ab,bp),d4=dot(ac,bp);if(d3>=0&&d4<=d3)return b;double vc=d1*d4-d3*d2;if(vc<=0&&d1>=0&&d3<=0)return add(a,ab,d1/(d1-d3));var cp=sub(p,c);double d5=dot(ab,cp),d6=dot(ac,cp);if(d6>=0&&d5<=d6)return c;double vb=d5*d2-d1*d6;if(vb<=0&&d2>=0&&d6<=0)return add(a,ac,d2/(d2-d6));double va=d3*d6-d5*d4;if(va<=0&&d4-d3>=0&&d5-d6>=0)return add(b,sub(c,b),(d4-d3)/((d4-d3)+(d5-d6)));double sum=va+vb+vc;if(Math.abs(sum)<1e-18)return a;return add(add(a,ab,vb/sum),ac,vc/sum);}
}
