package pzvrphysicality;

import java.nio.*;import java.security.*;import java.util.*;

/** Detached rigid geometry and triangle contact. Coordinates use world XY and physical height. */
public final class UtensilContact {
 public enum Region{WHOLE,LARGEST_COMPONENT,CHEESE_CUT,CARROT_ROOT,SOUP_SURFACE}
 public record Surface(MouthMath.Vec local,MouthMath.Vec world,double distance){}
 public record Geometry(float[] vertices,int[] faces){
  public Geometry{if(vertices==null||vertices.length<9||vertices.length>96000||vertices.length%3!=0||faces==null||faces.length<3||faces.length>192000||faces.length%3!=0)throw new IllegalArgumentException("Utensil geometry budget");
   vertices=vertices.clone();faces=faces.clone();for(float value:vertices)if(!Float.isFinite(value)||Math.abs(value)>10000)throw new IllegalArgumentException("Utensil geometry vertex");for(int index:faces)if(index<0||index>=vertices.length/3)throw new IllegalArgumentException("Utensil geometry face");}
  public float[] vertices(){return vertices.clone();}public int[] faces(){return faces.clone();}
 }
 private record Point(long x,long y,long z){}
 private static int root(int[] parents,int n){while(parents[n]!=n){parents[n]=parents[parents[n]];n=parents[n];}return n;}
 /** The profile verifies both the original mesh and this semantic subset before it can authorize contact. */
 public static Geometry region(Geometry source,Region region){
  if(source==null||region==null||source.faces.length>1536)throw new IllegalArgumentException("Edible region budget");
  if(region==Region.WHOLE)return source;
  int triangles=source.faces.length/3;boolean[] selected=new boolean[triangles];
  if(region==Region.LARGEST_COMPONENT){
   int[] parents=new int[triangles];for(int n=0;n<triangles;n++)parents[n]=n;var shared=new HashMap<Point,Integer>();
   for(int n=0;n<triangles;n++)for(int p=0;p<3;p++){int at=source.faces[n*3+p]*3;var key=new Point(Math.round((double)source.vertices[at]*1e6),Math.round((double)source.vertices[at+1]*1e6),Math.round((double)source.vertices[at+2]*1e6));var previous=shared.putIfAbsent(key,n);if(previous!=null)parents[root(parents,n)]=root(parents,previous);}
   var sizes=new HashMap<Integer,Integer>();for(int n=0;n<triangles;n++)sizes.merge(root(parents,n),1,Integer::sum);int winner=-1,best=0;boolean tied=false;
   for(var entry:sizes.entrySet()){if(entry.getValue()>best){winner=entry.getKey();best=entry.getValue();tied=false;}else if(entry.getValue()==best)tied=true;}
   if(tied||winner<0)throw new IllegalArgumentException("Ambiguous edible body component");for(int n=0;n<triangles;n++)selected[n]=root(parents,n)==winner;
  }else{
   int axis=region==Region.CARROT_ROOT?1:2;float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;for(int n=axis;n<source.vertices.length;n+=3){min=Math.min(min,source.vertices[n]);max=Math.max(max,source.vertices[n]);}
   double cut=min+(max-min)*.78;
   for(int n=0;n<triangles;n++){
    int a=source.faces[n*3]*3,b=source.faces[n*3+1]*3,c=source.faces[n*3+2]*3;
    if(region==Region.CARROT_ROOT)selected[n]=source.vertices[a+1]<=cut&&source.vertices[b+1]<=cut&&source.vertices[c+1]<=cut;
    else if(region==Region.SOUP_SURFACE){double lo=Math.min(source.vertices[a+2],Math.min(source.vertices[b+2],source.vertices[c+2])),hi=Math.max(source.vertices[a+2],Math.max(source.vertices[b+2],source.vertices[c+2]));selected[n]=hi-lo<1e-5&&lo>min+1e-5&&hi<max-1e-5;}
    else {double x=source.vertices[b]-source.vertices[a],y=source.vertices[b+1]-source.vertices[a+1],z=source.vertices[b+2]-source.vertices[a+2],u=source.vertices[c]-source.vertices[a],v=source.vertices[c+1]-source.vertices[a+1],w=source.vertices[c+2]-source.vertices[a+2];double nx=y*w-z*v,ny=z*u-x*w,nz=x*v-y*u,length=Math.sqrt(nx*nx+ny*ny+nz*nz);selected[n]=length>1e-12&&Math.abs(nx)>=length*.9;}
   }
  }
  int count=0;for(boolean included:selected)if(included)count++;if(count==0)throw new IllegalArgumentException("Original edible surface unavailable");var faces=new int[count*3];int at=0;for(int n=0;n<triangles;n++)if(selected[n])for(int p=0;p<3;p++)faces[at++]=source.faces[n*3+p];return new Geometry(source.vertices,faces);
 }
 private static int compare(long[] a,long[] b){for(int n=0;n<a.length;n++){int c=Long.compare(a[n],b[n]);if(c!=0)return c;}return 0;}
 /** Independent of UV seam duplicates, vertex ordering and triangle winding; mesh edits still invalidate it. */
 public static String fingerprint(Geometry geometry){
  var triangles=new ArrayList<long[]>();
  for(int n=0;n<geometry.faces.length;n+=3){
   long[][] points=new long[3][3];for(int p=0;p<3;p++)for(int axis=0;axis<3;axis++)points[p][axis]=Math.round((double)geometry.vertices[geometry.faces[n+p]*3+axis]*1_000_000);
   Arrays.sort(points,UtensilContact::compare);long[] triangle=new long[9];for(int p=0;p<3;p++)System.arraycopy(points[p],0,triangle,p*3,3);triangles.add(triangle);
  }
  triangles.sort(UtensilContact::compare);
  try{var digest=MessageDigest.getInstance("SHA-256");var buffer=ByteBuffer.allocate(72).order(ByteOrder.LITTLE_ENDIAN);long[] previous=null;
   for(var triangle:triangles){if(previous!=null&&compare(previous,triangle)==0)continue;buffer.clear();for(long value:triangle)buffer.putLong(value);digest.update(buffer.array());previous=triangle;}
   return HexFormat.of().formatHex(digest.digest());
  }catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
 }
 private static void matrix(float[] matrix){if(matrix==null||matrix.length!=16)throw new IllegalArgumentException("Utensil final matrix");for(float n:matrix)if(!Float.isFinite(n))throw new IllegalArgumentException("Utensil matrix number");if(Math.abs(matrix[3])+Math.abs(matrix[7])+Math.abs(matrix[11])+Math.abs(matrix[15]-1)>.0001)throw new IllegalArgumentException("Utensil affine matrix");}
 public static MouthMath.Vec world(MouthMath.Vec point,float[] matrix,MouthMath.Vec camera){
  matrix(matrix);if(point==null||camera==null)throw new IllegalArgumentException("Utensil point");
  double x=matrix[0]*point.x()+matrix[4]*point.y()+matrix[8]*point.z()+matrix[12];
  double y=matrix[1]*point.x()+matrix[5]*point.y()+matrix[9]*point.z()+matrix[13];
  double z=matrix[2]*point.x()+matrix[6]*point.y()+matrix[10]*point.z()+matrix[14];
  return new MouthMath.Vec(camera.x()-x,camera.y()-z,camera.z()+y);
 }
 private static MouthMath.Vec vertex(Geometry geometry,int index,float[] matrix,MouthMath.Vec camera){int at=index*3;return world(new MouthMath.Vec(geometry.vertices[at],geometry.vertices[at+1],geometry.vertices[at+2]),matrix,camera);}
 private static MouthMath.Vec subtract(MouthMath.Vec a,MouthMath.Vec b){return new MouthMath.Vec(a.x()-b.x(),a.y()-b.y(),a.z()-b.z());}
 private static double dot(MouthMath.Vec a,MouthMath.Vec b){return a.x()*b.x()+a.y()*b.y()+a.z()*b.z();}
 private static MouthMath.Vec along(MouthMath.Vec a,MouthMath.Vec b,double t){return new MouthMath.Vec(a.x()+b.x()*t,a.y()+b.y()*t,a.z()+b.z()*t);}
 private record Weights(double a,double b,double c){}
 private static double segmentAmount(MouthMath.Vec p,MouthMath.Vec a,MouthMath.Vec b){var ab=subtract(b,a);double length=dot(ab,ab);return length==0?0:Math.clamp(dot(subtract(p,a),ab)/length,0,1);}
 private static MouthMath.Vec blend(MouthMath.Vec a,MouthMath.Vec b,MouthMath.Vec c,Weights w){return new MouthMath.Vec(a.x()*w.a+b.x()*w.b+c.x()*w.c,a.y()*w.a+b.y()*w.b+c.y()*w.c,a.z()*w.a+b.z()*w.b+c.z()*w.c);}
 private static Weights weights(MouthMath.Vec p,MouthMath.Vec a,MouthMath.Vec b,MouthMath.Vec c){
  var ab=subtract(b,a);var ac=subtract(c,a);var ap=subtract(p,a);double d1=dot(ab,ap),d2=dot(ac,ap);
  if(d1<=0&&d2<=0)return new Weights(1,0,0);var bp=subtract(p,b);double d3=dot(ab,bp),d4=dot(ac,bp);if(d3>=0&&d4<=d3)return new Weights(0,1,0);
  double vc=d1*d4-d3*d2;if(vc<=0&&d1>=0&&d3<=0&&d1!=d3){double t=d1/(d1-d3);return new Weights(1-t,t,0);}
  var cp=subtract(p,c);double d5=dot(ab,cp),d6=dot(ac,cp);if(d6>=0&&d5<=d6)return new Weights(0,0,1);
  double vb=d5*d2-d1*d6;if(vb<=0&&d2>=0&&d6<=0&&d2!=d6){double t=d2/(d2-d6);return new Weights(1-t,0,t);}
  double va=d3*d6-d5*d4;if(va<=0&&d4-d3>=0&&d5-d6>=0&&(d4-d3)+(d5-d6)!=0){double t=(d4-d3)/((d4-d3)+(d5-d6));return new Weights(0,1-t,t);}
  double sum=va+vb+vc;if(Math.abs(sum)<1e-24){double x=segmentAmount(p,a,b),y=segmentAmount(p,b,c),z=segmentAmount(p,c,a);var choices=List.of(new Weights(1-x,x,0),new Weights(0,1-y,y),new Weights(z,0,1-z));return choices.stream().min(Comparator.comparingDouble(w->p.distance(blend(a,b,c,w)))).orElseThrow();}
  return new Weights(1-vb/sum-vc/sum,vb/sum,vc/sum);
 }
 public static double triangleDistance(MouthMath.Vec p,MouthMath.Vec a,MouthMath.Vec b,MouthMath.Vec c){return p.distance(blend(a,b,c,weights(p,a,b,c)));}
 private static MouthMath.Vec local(Geometry geometry,int index){int at=index*3;return new MouthMath.Vec(geometry.vertices[at],geometry.vertices[at+1],geometry.vertices[at+2]);}
 public static Surface closest(MouthMath.Vec tip,Geometry food,float[] matrix,MouthMath.Vec camera){
  if(tip==null||food==null)return null;matrix(matrix);Surface best=null;
  for(int n=0;n<food.faces.length;n+=3){var a=local(food,food.faces[n]);var b=local(food,food.faces[n+1]);var c=local(food,food.faces[n+2]);var wa=world(a,matrix,camera);var wb=world(b,matrix,camera);var wc=world(c,matrix,camera);var weights=weights(tip,wa,wb,wc);var point=blend(wa,wb,wc,weights);double distance=tip.distance(point);if(best==null||distance<best.distance)best=new Surface(blend(a,b,c,weights),point,distance);}return best;
 }
 public static boolean touching(MouthMath.Vec tip,Geometry food,float[] matrix,MouthMath.Vec camera,double tolerance){
  if(tip==null||food==null||!Double.isFinite(tolerance)||tolerance<=0||tolerance>.2)return false;matrix(matrix);
  for(int n=0;n<food.faces.length;n+=3)if(triangleDistance(tip,vertex(food,food.faces[n],matrix,camera),vertex(food,food.faces[n+1],matrix,camera),vertex(food,food.faces[n+2],matrix,camera))<=tolerance)return true;
  return false;
 }
 private UtensilContact(){}
}
