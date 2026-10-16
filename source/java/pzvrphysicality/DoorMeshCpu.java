package pzvrphysicality;

import java.lang.reflect.*;import java.util.*;
import zombie.core.skinnedmodel.model.ModelMesh;import zombie.core.skinnedmodel.model.VertexBufferObject;

/** Bounded private numeric copies of original door GLB meshes before their normal GPU upload. */
public final class DoorMeshCpu {
 public record Data(float[] positions,int[] faces,float[] weights,int[] joints){
  public Data{
   if(positions==null||faces==null||weights==null||joints==null||positions.length<12||positions.length%3!=0||positions.length>24000||faces.length<3||faces.length%3!=0||faces.length>48000||weights.length!=positions.length/3*4||joints.length!=weights.length)throw new IllegalArgumentException("Door mesh extent");
   positions=positions.clone();faces=faces.clone();weights=weights.clone();joints=joints.clone();
   for(float p:positions)if(!Float.isFinite(p)||Math.abs(p)>16)throw new IllegalArgumentException("Door mesh coordinate");for(int f:faces)if(f<0||f>=positions.length/3)throw new IllegalArgumentException("Door mesh index");
   for(int v=0;v<positions.length/3;v++){float sum=0;for(int k=0;k<4;k++){int i=v*4+k;float w=weights[i];if(!Float.isFinite(w)||w<0||w>1.002f||w>0&&(joints[i]<0||joints[i]>=64))throw new IllegalArgumentException("Door skin weight/index");sum+=w;}if(sum>0&&Math.abs(sum-1)>.002f)throw new IllegalArgumentException("Door skin total");}
  }
  public float[] positions(){return positions.clone();}public int[] faces(){return faces.clone();}public float[] weights(){return weights.clone();}public int[] joints(){return joints.clone();}
  int bytes(){return (positions.length+faces.length+weights.length+joints.length)*4;}
  public boolean weighted(){for(float w:weights)if(w>0)return true;return false;}
 }
 private static final WeakHashMap<ModelMesh,Data> cache=new WeakHashMap<>();
 private static final WeakHashMap<ModelMesh,BasileDoorAssets.Receipt> basile=new WeakHashMap<>();private static volatile boolean basileSeen;private static long captured,rejected;
 public static boolean basileReady(){return basileSeen;}
 private static boolean basilePath(String path){return path!=null&&(path.startsWith("BasileDoors3D/")||path.startsWith("media/models_X/BasileDoors3D/"));}
 public static boolean allowed(String path){
  if(path==null||path.length()>512||path.indexOf('\0')>=0)return false;String p=path.replace('\\','/').toLowerCase(Locale.ROOT);if(p.contains("..")||p.contains(":")||p.contains("?")||p.contains("#"))return false;
  int at=p.indexOf("isoobject/");if(at<0||at>0&&p.charAt(at-1)!='/')return false;String name=p.substring(at+10);if(name.contains("/"))return false;
  String plain=name.replaceFirst("\\.(glb|x|txt)$","");return plain.startsWith("door_")||plain.startsWith("fixtures_doors_")||Set.of("fixtures_bathroom_01_48","fixtures_bathroom_01_49","carpentry_01_48","carpentry_01_49").contains(plain);
 }
 private static Object read(Object source,String name)throws ReflectiveOperationException{var f=source.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(source);}
 public static void capture(Object processed,ModelMesh mesh){
  if(!Main.ready()||mesh==null)return; synchronized(DoorMeshCpu.class){cache.remove(mesh);basile.remove(mesh);}
  try{
   String path=mesh.getPath().getPath();boolean nativePath=allowed(path);BasileDoorAssets.Receipt proof=null;
   if(!nativePath){if(!basilePath(path))return;basileSeen=true;proof=BasileDoorAssets.capture(path);if(proof==null)return;}
   if(processed==null||!processed.getClass().getName().equals("zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene"))return;
   Object source=read(processed,"skinnedMesh");if(source==null){var statics=(List<?>)read(processed,"staticMeshes");if(statics.size()!=1)return;source=statics.getFirst();}
   boolean skin=source.getClass().getName().equals("zombie.core.skinnedmodel.model.jassimp.ImportedSkinnedMesh");if(!skin&&!source.getClass().getName().equals("zombie.core.skinnedmodel.model.jassimp.ImportedStaticMesh"))return;
   var array=(VertexBufferObject.VertexArray)read(source,skin?"vertices":"verticesUnskinned");var faces=(int[])read(source,"elements");if(array==null||array.numVertices<4||array.numVertices>8000)throw new IllegalArgumentException("Door CPU source extent");
   var format=array.format;int p=format.indexOf(VertexBufferObject.VertexType.VertexArray),w=format.indexOf(VertexBufferObject.VertexType.BlendWeightArray),j=format.indexOf(VertexBufferObject.VertexType.BlendIndexArray);
   if(p<0||format.getElement(p).byteSize!=12||skin&&(w<0||j<0||format.getElement(w).byteSize!=16||format.getElement(j).byteSize!=16))throw new IllegalArgumentException("Door CPU vertex layout");
   float[] positions=new float[array.numVertices*3],weights=new float[array.numVertices*4];int[] joints=new int[weights.length];
   for(int v=0;v<array.numVertices;v++){for(int c=0;c<3;c++)positions[v*3+c]=array.getElementFloat(v,p,c);if(skin)for(int c=0;c<4;c++){weights[v*4+c]=array.getElementFloat(v,w,c);float bone=array.getElementFloat(v,j,c);if(!Float.isFinite(bone)||bone!=Math.rint(bone))throw new IllegalArgumentException("Door CPU bone index");joints[v*4+c]=(int)bone;}}
   store(mesh,new Data(positions,faces,weights,joints),proof);
  }catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){synchronized(DoorMeshCpu.class){rejected++;}}
 }
 public static synchronized void offer(ModelMesh mesh,Data data){store(mesh,data,null);}
 private static synchronized void store(ModelMesh mesh,Data data,BasileDoorAssets.Receipt proof){
  Objects.requireNonNull(mesh);Objects.requireNonNull(data);if(proof!=null&&!proof.current())return;int bytes=cache.values().stream().mapToInt(Data::bytes).sum();var old=cache.get(mesh);if(old!=null)bytes-=old.bytes();if(cache.size()>=64&&!cache.containsKey(mesh)||bytes+data.bytes()>8*1024*1024){cache.clear();basile.clear();}cache.put(mesh,data);if(proof==null)basile.remove(mesh);else basile.put(mesh,proof);captured++;
 }
 public static synchronized Data get(ModelMesh mesh){var proof=basile.get(mesh);if(proof!=null&&(!proof.current()||!Objects.equals(proof.key,BasileDoorAssets.key(mesh.getPath().getPath())))||proof==null&&mesh!=null&&basilePath(mesh.getPath().getPath())){cache.remove(mesh);basile.remove(mesh);return null;}if(proof!=null&&!mesh.isReady())return null;return cache.get(mesh);}
 public static synchronized void clear(){cache.clear();basile.clear();basileSeen=false;captured=rejected=0;}
 /** Same weighted vec4 rows as the pinned model.vert; never normalizes the original weights. */
 public static float[] skin(Data data,float[] palette,int start){
  Objects.requireNonNull(data);var positions=data.positions();if(start<0||!data.weighted())return positions;if(palette==null||start>palette.length/12||palette.length%12!=0||palette.length>4096*12)throw new IllegalArgumentException("Door palette extent");
  for(int v=0;v<positions.length/3;v++){double x=0,y=0,z=0;boolean weighted=false;for(int k=0;k<4;k++){int at=v*4+k;float w=data.weights[at];if(w<=0)continue;weighted=true;int bone=start+data.joints[at];if(bone<0||bone>=palette.length/12)throw new IllegalArgumentException("Door palette index");int row=bone*12;for(int c=0;c<12;c++)if(!Float.isFinite(palette[row+c]))throw new IllegalArgumentException("Door palette row");double px=data.positions[v*3],py=data.positions[v*3+1],pz=data.positions[v*3+2];x+=w*(palette[row]*px+palette[row+1]*py+palette[row+2]*pz+palette[row+3]);y+=w*(palette[row+4]*px+palette[row+5]*py+palette[row+6]*pz+palette[row+7]);z+=w*(palette[row+8]*px+palette[row+9]*py+palette[row+10]*pz+palette[row+11]);}if(weighted){positions[v*3]=(float)x;positions[v*3+1]=(float)y;positions[v*3+2]=(float)z;}}
  return positions;
 }
 public static float[] skin(Data data,float[] palette,int start,int bones){
  if(start<0||!data.weighted())return data.positions();if(palette==null||bones<0||bones>4096||bones>palette.length/12||start>=bones)throw new IllegalArgumentException("Door active palette extent");
  int needed=0;for(int i=0;i<data.weights.length;i++)if(data.weights[i]>0)needed=Math.max(needed,data.joints[i]+1);
  if(needed>bones-start)throw new IllegalArgumentException("Door active palette index");return skin(data,Arrays.copyOfRange(palette,start*12,(start+needed)*12),0);
 }
 public static synchronized String status(){return "door CPU meshes "+cache.size()+", bytes "+cache.values().stream().mapToInt(Data::bytes).sum()+", copies "+captured+", rejected "+rejected;}
 private DoorMeshCpu(){}
}
