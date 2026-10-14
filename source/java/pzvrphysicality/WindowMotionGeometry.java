package pzvrphysicality;

import java.util.*;

/** Bounded immutable geometric adaptation. Native endpoint/provider admission belongs to the caller. */
public final class WindowMotionGeometry {
 public record Pieces(boolean north,float plane,float[] fixed,float[] lower,float[] fixedGlass,float[] lowerGlass,float[] bounds){
  public Pieces {new NativeWindowGeometry.Data(north,plane,fixed,fixedGlass,bounds);new NativeWindowGeometry.Data(north,plane,lower,lowerGlass,bounds);fixed=fixed.clone();lower=lower.clone();fixedGlass=fixedGlass.clone();lowerGlass=lowerGlass.clone();bounds=bounds.clone();}
  public float[] fixed(){return fixed.clone();}public float[] lower(){return lower.clone();}public float[] fixedGlass(){return fixedGlass.clone();}public float[] lowerGlass(){return lowerGlass.clone();}public float[] bounds(){return bounds.clone();}
 }
 public record Progressed(double progress,float[] fixed,float[] lower,float[] glass,NativeWindowGeometry.Data contacts){
  public Progressed {WindowMotionProfile.validate(progress);fixed=fixed.clone();lower=lower.clone();glass=glass.clone();Objects.requireNonNull(contacts);}
  public float[] fixed(){return fixed.clone();}public float[] lower(){return lower.clone();}public float[] glass(){return glass.clone();}
 }
 private static final class Rows {
  final ArrayList<Float> values=new ArrayList<>();
  void triangle(float[] a,float[] b,float[] c){if(values.size()+24>NativeWindowGeometry.MAX_FLOATS)throw new IllegalArgumentException("Window piece budget");for(var v:new float[][]{a,b,c})for(float value:v)values.add(value);}
  void polygon(List<float[]> polygon){for(int n=1;n+1<polygon.size();n++)triangle(polygon.getFirst(),polygon.get(n),polygon.get(n+1));}
  float[] array(){var out=new float[values.size()];for(int n=0;n<out.length;n++)out[n]=values.get(n);return out;}
 }
 private record Half(int axis,float limit,boolean greater){}
 private static List<float[]> clip(List<float[]> source,Half half){
  if(source.isEmpty())return source;var result=new ArrayList<float[]>();var a=source.getLast();double da=(a[half.axis]-half.limit)*(half.greater?1:-1);
  for(var b:source){double db=(b[half.axis]-half.limit)*(half.greater?1:-1);if((da>=0)!=(db>=0)){double weight=da/(da-db);var cut=new float[8];for(int axis=0;axis<8;axis++)cut[axis]=(float)(a[axis]+(b[axis]-a[axis])*weight);cut[half.axis]=half.limit;result.add(cut);}if(db>=0)result.add(b);a=b;da=db;}
  return result;
 }
 private static float[] band(float[] mesh,Half... halves){var rows=new Rows();for(int at=0;at<mesh.length;at+=24){var polygon=new ArrayList<float[]>();for(int n=0;n<3;n++)polygon.add(Arrays.copyOfRange(mesh,at+n*8,at+(n+1)*8));List<float[]> result=polygon;for(var half:halves)result=clip(result,half);rows.polygon(result);}return rows.array();}
 public static float[] inner(WindowMotionProfile profile,float[] mesh){int along=profile.north()?0:2;return band(mesh,new Half(along,profile.left(),true),new Half(along,profile.right(),false),new Half(1,profile.lower(),true),new Half(1,profile.upper(),false));}
 public static float[] outer(WindowMotionProfile profile,Pieces pieces){int along=profile.north()?0:2;var mesh=join(pieces.fixed(),pieces.lower());var left=new Half(along,profile.left(),true);var right=new Half(along,profile.right(),false);return join(band(mesh,new Half(along,profile.left(),false)),band(mesh,left,new Half(along,profile.right(),true)),band(mesh,left,right,new Half(1,profile.lower(),false)),band(mesh,left,right,new Half(1,profile.upper(),true)));}
 private static float[] join(float[]... arrays){int length=0;for(var array:arrays)length=Math.addExact(length,array.length);if(length>NativeWindowGeometry.MAX_FLOATS)throw new IllegalArgumentException("Window combined budget");var out=new float[length];int at=0;for(var array:arrays){System.arraycopy(array,0,out,at,array.length);at+=array.length;}return out;}
 public static Pieces split(WindowMotionProfile profile,NativeWindowGeometry.Data closed){
  Objects.requireNonNull(profile);Objects.requireNonNull(closed);if(profile.north()!=closed.north()||Math.abs(closed.plane()+.5f)>.0001f)throw new IllegalArgumentException("Window profile orientation/plane");
  int along=closed.north()?0:2;var bounds=closed.bounds();if(bounds[along]>profile.left()||bounds[along+3]<profile.right()||bounds[1]>profile.lower()||bounds[4]<profile.upper())throw new IllegalArgumentException("Window profile outside original geometry");
  var left=new Half(along,profile.left(),true);var right=new Half(along,profile.right(),false);var low=new Half(1,profile.lower(),true);var middle=new Half(1,profile.middle(),false);
  var source=closed.mesh();var moving=band(source,left,right,low,new Half(1,profile.middle()+3/64f,false));
  // Disjoint source partition preserves the outer frame and the complete fixed upper region.
  var fixed=join(band(source,new Half(along,profile.left(),false)),band(source,left,new Half(along,profile.right(),true)),band(source,left,right,new Half(1,profile.lower(),false)),band(source,left,right,low,new Half(1,profile.middle(),true)));
  var aperture=closed.aperture();var fixedGlass=band(aperture,new Half(1,profile.middle(),true));var lowerGlass=band(aperture,left,right,new Half(1,profile.glassLower(),true),middle);
  if(moving.length==0||fixed.length==0||fixedGlass.length==0||lowerGlass.length==0)throw new IllegalArgumentException("Window measured pieces absent");
  return new Pieces(closed.north(),closed.plane(),fixed,moving,fixedGlass,lowerGlass,bounds);
 }
 public static Progressed progress(WindowMotionProfile profile,Pieces pieces,double progress,boolean smashed,boolean removed,boolean destroyed){
  Objects.requireNonNull(profile);Objects.requireNonNull(pieces);WindowMotionProfile.validate(progress);if(profile.north()!=pieces.north)throw new IllegalArgumentException("Window piece orientation");var lower=pieces.lower();var lowerGlass=pieces.lowerGlass();float rise=(float)(profile.travel()*progress);
  for(var rows:new float[][]{lower,lowerGlass})for(int at=0;at<rows.length;at+=8)rows[at+1]+=rise;
  // The native upper baked pane is one visible/contact surface, not two alpha layers.
  var visibleLowerGlass=band(lowerGlass,new Half(1,profile.middle(),false));var glass=smashed||removed||destroyed?new float[0]:join(pieces.fixedGlass(),visibleLowerGlass);
  var mesh=join(pieces.fixed(),lower);var bounds=pieces.bounds();for(int at=0;at<mesh.length;at+=8)bounds[4]=Math.max(bounds[4],mesh[at+1]);var contact=new NativeWindowGeometry.Data(pieces.north,pieces.plane,mesh,glass,bounds);
  return new Progressed(progress,pieces.fixed(),lower,glass,contact);
 }
 /** Native endpoint meshes are retained exactly. Only the measured remaining upper glass is selected when open. */
 public static Progressed endpoint(WindowMotionProfile profile,Progressed progressed,NativeWindowGeometry.Data closed,NativeWindowGeometry.Data open){
  if(progressed.progress()!=0&&progressed.progress()!=1)return progressed;var original=progressed.progress()==0?closed:open;if(original==null||original.north()!=profile.north())throw new IllegalArgumentException("Window endpoint source");var glass=progressed.progress()==0?original.aperture():band(original.aperture(),new Half(1,profile.glassLower()+profile.travel(),true));var data=new NativeWindowGeometry.Data(original.north(),original.plane(),original.mesh(),glass,original.bounds());return new Progressed(progressed.progress(),original.mesh(),new float[0],glass,data);
 }
 private WindowMotionGeometry(){}
}
