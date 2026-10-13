package pzvrphysicality;

import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** A detached crop of the original edible surface, mounted in the utensil's local space. */
public final class BiteGeometry {
 public record Mesh(float[] vertices,int[] faces){
  public Mesh{
   if(vertices==null||faces==null||vertices.length<24||vertices.length%8!=0||vertices.length>32768||faces.length<3||faces.length%3!=0||faces.length>12288)throw new IllegalArgumentException("Bite mesh extent");
   vertices=vertices.clone();faces=faces.clone();for(float value:vertices)if(!Float.isFinite(value))throw new IllegalArgumentException("Bite vertex");
   for(int index:faces)if(index<0||index>=vertices.length/8)throw new IllegalArgumentException("Bite face");
  }
  public float[] vertices(){return vertices.clone();}public int[] faces(){return faces.clone();}
 }
 private record Vertex(Vector3f p,Vector3f n,float u,float v){
  Vertex mix(Vertex other,float amount){return new Vertex(new Vector3f(p).lerp(other.p,amount),new Vector3f(n).lerp(other.n,amount),u+(other.u-u)*amount,v+(other.v-v)*amount);}
 }
 private record Polygon(Vertex[] vertices,float cap){}
 private record Key(int x,int y,int z) implements Comparable<Key>{
  static Key of(Vector3f p){return new Key(bits(p.x),bits(p.y),bits(p.z));}
  private static int bits(float value){return value==0?0:Float.floatToIntBits(value);}
  public int compareTo(Key other){int value=Integer.compareUnsigned(x,other.x);if(value==0)value=Integer.compareUnsigned(y,other.y);return value==0?Integer.compareUnsigned(z,other.z):value;}
 }
 private record Edge(Key a,Key b){static Edge of(Key a,Key b){return a.compareTo(b)<0?new Edge(a,b):new Edge(b,a);}}
 private record Cut(Vertex a,Vertex b){}
 private record Component(List<Polygon> polygons,boolean cap){}
 public record Portion(float[] triangles){
  public Portion{if(triangles==null||triangles.length==0||triangles.length%27!=0||triangles.length>180000)throw new IllegalArgumentException("Bite portion extent");triangles=triangles.clone();for(float value:triangles)if(!Float.isFinite(value))throw new IllegalArgumentException("Bite portion vertex");}
  public float[] triangles(){return triangles.clone();}
 }
 public static Portion recenter(Portion source,Vector3f from,Vector3f to){
  if(source==null||from==null||to==null||!from.isFinite()||!to.isFinite())throw new IllegalArgumentException("Bite mount centre");var delta=new Vector3f(to).sub(from);var values=source.triangles();for(int n=0;n<values.length;n+=9){values[n]+=delta.x;values[n+1]+=delta.y;values[n+2]+=delta.z;}return new Portion(values);
 }
 private static float axis(Vector3f v,int axis){return axis==0?v.x:axis==1?v.y:v.z;}
 private static Vector3f normal(int axis,float sign){return axis==0?new Vector3f(sign,0,0):axis==1?new Vector3f(0,sign,0):new Vector3f(0,0,sign);}
 private static boolean usable(Matrix4f matrix){return matrix!=null&&matrix.isFinite()&&Math.abs(matrix.determinant())>1e-10f;}
 public static Portion crop(Mesh source,Matrix4f food,Matrix4f tool,Vector3f centre,float radius){
  if(source==null||!usable(food)||!usable(tool)||centre==null||!centre.isFinite()||!Float.isFinite(radius)||radius<.0005f||radius>.1f)throw new IllegalArgumentException("Bite crop transform");
  var inverse=new Matrix4f(tool).invert();var foodNormal=new org.joml.Matrix3f(food).invert().transpose();var toolNormal=new org.joml.Matrix3f(tool).transpose();
  var sourceVertices=new Vertex[source.vertices.length/8];var welds=new Key[sourceVertices.length];
  for(int n=0;n<sourceVertices.length;n++){
   int at=n*8;var original=new Vector3f(source.vertices[at],source.vertices[at+1],source.vertices[at+2]);welds[n]=Key.of(original);var point=food.transformPosition(new Vector3f(original));
   var direction=foodNormal.transform(new Vector3f(source.vertices[at+3],source.vertices[at+4],source.vertices[at+5]));
   if(!point.isFinite()||!direction.isFinite())throw new IllegalArgumentException("Bite source transform");
   sourceVertices[n]=new Vertex(point,direction,source.vertices[at+6],source.vertices[at+7]);
  }
  var component=component(sourceVertices,welds,source.faces,centre);List<Polygon> polygons=component.polygons;
  var result=new ArrayList<Float>();
  for(int plane=0;plane<6;plane++){
   int axis=plane/2;float sign=plane%2==0?-1:1;float limit=axis(centre,axis)+sign*radius;
   var kept=new ArrayList<Polygon>();var cuts=new ArrayList<Cut>();
   for(Polygon sourcePolygon:polygons){Vertex[] polygon=sourcePolygon.vertices;var clipped=new ArrayList<Vertex>();for(int n=0;n<polygon.length;n++){
    Vertex a=polygon[n],b=polygon[(n+1)%polygon.length];float da=sign*(axis(a.p,axis)-limit),db=sign*(axis(b.p,axis)-limit);boolean insideA=da<=0,insideB=db<=0;
    if(insideA)clipped.add(a);if(insideA!=insideB)clipped.add(intersection(a,b,da,db,axis,limit));
   }
   clipped=clean(clipped);if(clipped.size()>=3){kept.add(new Polygon(clipped.toArray(Vertex[]::new),sourcePolygon.cap));
    if(component.cap){var endpoints=new LinkedHashMap<Key,Vertex>();for(Vertex vertex:clipped)if(axis(vertex.p,axis)==limit)endpoints.putIfAbsent(Key.of(vertex.p),vertex);
     // A convex source polygon has one cut segment. Its edge order comes from clipping, never angle sorting.
     boolean crossed=false;for(Vertex vertex:polygon)if(sign*(axis(vertex.p,axis)-limit)>0){crossed=true;break;}
     if(crossed&&endpoints.size()==2){var values=endpoints.values().iterator();cuts.add(new Cut(values.next(),values.next()));if(cuts.size()>256)throw new IllegalArgumentException("Bite cut-edge budget");}
     else if(crossed&&endpoints.size()>2)throw new IllegalArgumentException("Ambiguous bite cut polygon");
    }
   }}
   if(component.cap&&!cuts.isEmpty()){var cap=cap(cuts,axis,sign);if(cap!=null)kept.add(cap);}
   polygons=kept;if(polygons.isEmpty())throw new IllegalArgumentException("No edible surface at the contact");
  }
  for(Polygon polygon:polygons)for(int n=1;n+1<polygon.vertices.length;n++)triangle(result,polygon.vertices[0],polygon.vertices[n],polygon.vertices[n+1],inverse,toolNormal,polygon.cap);
  if(result.isEmpty())throw new IllegalArgumentException("Degenerate edible portion");var output=new float[result.size()];for(int n=0;n<output.length;n++)output[n]=result.get(n);return new Portion(output);
 }
 private static Vertex intersection(Vertex a,Vertex b,float da,float db,int axis,float limit){
  float amount=Math.max(0,Math.min(1,da/(da-db)));var mixed=a.mix(b,amount);boolean ordered=Key.of(a.p).compareTo(Key.of(b.p))<0;
  Vertex low=ordered?a:b,high=ordered?b:a;float dl=ordered?da:db,dh=ordered?db:da;
  var point=new Vector3f(low.p).lerp(high.p,Math.max(0,Math.min(1,dl/(dl-dh))));if(axis==0)point.x=limit;else if(axis==1)point.y=limit;else point.z=limit;
  return new Vertex(point,mixed.n,mixed.u,mixed.v);
 }
 private static ArrayList<Vertex> clean(List<Vertex> values){var result=new ArrayList<Vertex>();for(Vertex value:values)if(result.isEmpty()||!Key.of(result.getLast().p).equals(Key.of(value.p)))result.add(value);if(result.size()>1&&Key.of(result.getFirst().p).equals(Key.of(result.getLast().p)))result.removeLast();return result;}
 private static int root(int[] parents,int index){while(parents[index]!=index){parents[index]=parents[parents[index]];index=parents[index];}return index;}
 private static void join(int[] parents,int a,int b){parents[root(parents,a)]=root(parents,b);}
 private static Component component(Vertex[] vertices,Key[] welds,int[] faces,Vector3f centre){
  int[] parents=new int[vertices.length];var welded=new HashMap<Key,Integer>();for(int n=0;n<parents.length;n++){parents[n]=n;Integer before=welded.putIfAbsent(welds[n],n);if(before!=null)join(parents,n,before);}
  var valid=new ArrayList<int[]>();for(int n=0;n<faces.length;n+=3){int a=faces[n],b=faces[n+1],c=faces[n+2];var cross=new Vector3f(vertices[b].p).sub(vertices[a].p).cross(new Vector3f(vertices[c].p).sub(vertices[a].p));if(!cross.isFinite())throw new IllegalArgumentException("Bite source scale");if(cross.lengthSquared()<1e-16f)continue;join(parents,a,b);join(parents,a,c);valid.add(new int[]{a,b,c});}
  var groups=new HashMap<Integer,List<int[]>>();for(int[] triangle:valid)groups.computeIfAbsent(root(parents,triangle[0]),ignored->new ArrayList<>()).add(triangle);
  List<int[]> chosen=null;float closest=Float.POSITIVE_INFINITY;boolean ambiguous=false;
  for(var group:groups.values()){float distance=Float.POSITIVE_INFINITY;for(var triangle:group)distance=Math.min(distance,distance(centre,vertices[triangle[0]].p,vertices[triangle[1]].p,vertices[triangle[2]].p));
   if(distance<closest-1e-12f){closest=distance;chosen=group;ambiguous=false;}else if(Math.abs(distance-closest)<=1e-12f)ambiguous=true;
  }
  if(chosen==null||!Float.isFinite(closest)||ambiguous)throw new IllegalArgumentException("Ambiguous nearest edible component");
  var polygons=new ArrayList<Polygon>();var counts=new HashMap<Edge,Integer>();var points=new HashMap<Key,Vector3f>();for(var triangle:chosen){var polygon=new Polygon(new Vertex[]{vertices[triangle[0]],vertices[triangle[1]],vertices[triangle[2]]},0);polygons.add(polygon);for(int n=0;n<3;n++){Key a=welds[triangle[n]],b=welds[triangle[(n+1)%3]];points.putIfAbsent(a,polygon.vertices[n].p);counts.merge(Edge.of(a,b),1,Integer::sum);}}
  var first=polygons.getFirst().vertices;var outward=new Vector3f(first[1].p).sub(first[0].p).cross(new Vector3f(first[2].p).sub(first[0].p)).normalize();boolean planar=true;
  for(var point:points.values())if(Math.abs(new Vector3f(point).sub(first[0].p).dot(outward))>1e-7f){planar=false;break;}
  boolean closed=!planar&&counts.values().stream().allMatch(value->value==2);return new Component(polygons,closed);
 }
 private static float distance(Vector3f p,Vector3f a,Vector3f b,Vector3f c){
  var ab=new Vector3f(b).sub(a);var ac=new Vector3f(c).sub(a);var ap=new Vector3f(p).sub(a);float d1=ab.dot(ap),d2=ac.dot(ap);if(d1<=0&&d2<=0)return p.distanceSquared(a);
  var bp=new Vector3f(p).sub(b);float d3=ab.dot(bp),d4=ac.dot(bp);if(d3>=0&&d4<=d3)return p.distanceSquared(b);
  float vc=d1*d4-d3*d2;if(vc<=0&&d1>=0&&d3<=0)return p.distanceSquared(new Vector3f(a).fma(d1/(d1-d3),ab));
  var cp=new Vector3f(p).sub(c);float d5=ab.dot(cp),d6=ac.dot(cp);if(d6>=0&&d5<=d6)return p.distanceSquared(c);
  float vb=d5*d2-d1*d6;if(vb<=0&&d2>=0&&d6<=0)return p.distanceSquared(new Vector3f(a).fma(d2/(d2-d6),ac));
  float va=d3*d6-d5*d4;if(va<=0&&d4-d3>=0&&d5-d6>=0)return p.distanceSquared(new Vector3f(b).fma((d4-d3)/((d4-d3)+(d5-d6)),new Vector3f(c).sub(b)));
  float inverse=1/(va+vb+vc);return p.distanceSquared(new Vector3f(a).fma(vb*inverse,ab).fma(vc*inverse,ac));
 }
 private static Polygon cap(List<Cut> cuts,int axis,float sign){
  var points=new TreeMap<Key,Vertex>();var adjacent=new HashMap<Key,List<Key>>();var edges=new HashSet<Edge>();
  for(var cut:cuts){Key a=Key.of(cut.a.p),b=Key.of(cut.b.p);if(a.equals(b))continue;if(!edges.add(Edge.of(a,b)))throw new IllegalArgumentException("Duplicate bite cut edge");points.putIfAbsent(a,cut.a);points.putIfAbsent(b,cut.b);adjacent.computeIfAbsent(a,ignored->new ArrayList<>()).add(b);adjacent.computeIfAbsent(b,ignored->new ArrayList<>()).add(a);}
  if(points.size()<3)return null;if(points.size()>256||adjacent.values().stream().anyMatch(value->value.size()!=2))throw new IllegalArgumentException("Open or ambiguous bite cut loop");
  var loop=new ArrayList<Vertex>();var visited=new HashSet<Key>();Key start=points.firstKey(),previous=null,current=start;
  do{if(!visited.add(current))throw new IllegalArgumentException("Repeated bite cut loop");loop.add(points.get(current));var neighbors=adjacent.get(current);Key next=neighbors.getFirst().equals(previous)?neighbors.getLast():neighbors.getFirst();previous=current;current=next;}while(!current.equals(start));
  if(visited.size()!=points.size())throw new IllegalArgumentException("Multiple bite cut loops");
  int a=(axis+1)%3,b=(axis+2)%3;var origin=loop.getFirst().p;double area=0,ulp=0,span=0;for(int n=0;n<loop.size();n++){var p=loop.get(n).p;var q=loop.get((n+1)%loop.size()).p;area+=((double)axis(p,a)-axis(origin,a))*((double)axis(q,b)-axis(origin,b))-((double)axis(q,a)-axis(origin,a))*((double)axis(p,b)-axis(origin,b));ulp=Math.max(ulp,Math.max(Math.ulp(axis(p,a)),Math.ulp(axis(p,b))));span=Math.max(span,p.distance(origin));}
  double epsilon=Math.max(1e-12,ulp*span*4);if(Math.abs(area)<=epsilon)return null;double direction=Math.signum(area);
  for(int n=0;n<loop.size();n++){var p=loop.get(n).p;var q=loop.get((n+1)%loop.size()).p;for(var vertex:loop){var r=vertex.p;double cross=((double)axis(q,a)-axis(p,a))*((double)axis(r,b)-axis(p,b))-((double)axis(q,b)-axis(p,b))*((double)axis(r,a)-axis(p,a));if(cross*direction < -epsilon)throw new IllegalArgumentException("Nonconvex bite cut loop");}}
  if(direction!=sign)Collections.reverse(loop);var outward=normal(axis,sign);var vertices=new Vertex[loop.size()];for(int n=0;n<vertices.length;n++){var vertex=loop.get(n);vertices[n]=new Vertex(vertex.p,outward,vertex.u,vertex.v);}return new Polygon(vertices,1);
 }
 private static void triangle(List<Float> target,Vertex a,Vertex b,Vertex c,Matrix4f inverse,org.joml.Matrix3f normals,float cap){
  if(new Vector3f(b.p).sub(a.p).cross(new Vector3f(c.p).sub(a.p)).lengthSquared()<1e-16f)return;
  for(Vertex vertex:new Vertex[]{a,b,c}){
   var point=inverse.transformPosition(new Vector3f(vertex.p));var n=normals.transform(new Vector3f(vertex.n));if(n.lengthSquared()<1e-12f)n.set(0,1,0);else n.normalize();
   for(float value:new float[]{point.x,point.y,point.z,n.x,n.y,n.z,vertex.u,vertex.v,cap})target.add(value);
  }if(target.size()>180000)throw new IllegalArgumentException("Bite output budget");
 }
 private BiteGeometry(){}
}
