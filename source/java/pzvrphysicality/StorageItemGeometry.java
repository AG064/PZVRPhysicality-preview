package pzvrphysicality;
import java.lang.reflect.Field;import java.util.*;
import viewpoint.core.Frame;import viewpoint.render.ModelDraws;
import zombie.core.skinnedmodel.model.*;
import zombie.inventory.InventoryItem;
import viewpointvr.physics.CollisionShape;

/** Captures the same rigid model records that are drawn, removing the body's pose before solver preparation. */
public final class StorageItemGeometry {
 private static Field meshesField,valuesField;
 private record Cached(List<ModelMesh> meshes,long[] basis,CollisionShape shape){}
 private static final WeakHashMap<InventoryItem,Cached> cache=new WeakHashMap<>();
 public static CollisionShape read(Frame frame,int first,int last,float[] body,InventoryItem item)throws ReflectiveOperationException{
  if(meshesField==null){meshesField=ModelDraws.class.getDeclaredField("meshes");valuesField=ModelDraws.class.getDeclaredField("values");meshesField.setAccessible(true);valuesField.setAccessible(true);}
  if(first<0||last<=first||last-first>16)throw new IllegalArgumentException("Cached storage geometry range");
  var meshes=(ModelMesh[])meshesField.get(frame.scene.models);var values=(float[])valuesField.get(frame.scene.models);if(last>meshes.length||last>values.length/68)throw new IllegalArgumentException("Cached storage geometry extent");
  var refs=new ArrayList<ModelMesh>();long[] signature=new long[(last-first)*18];int at=0;
  for(int n=first;n<last;n++){
   var mesh=meshes[n];if(mesh==null)throw new IllegalArgumentException("Storage geometry mesh");refs.add(mesh);
   var basis=ItemMesh.fromRendered(new float[]{0,0,0,.001f,0,0,0,.001f,0,0,0,.001f},new int[]{0,2,1},Arrays.copyOfRange(values,n*68,n*68+16),frame.camX,frame.camY,frame.camZ,body).vertices();
   for(int axis=0;axis<3;axis++)signature[at++]=Math.round(basis[axis]*500);
   for(int point=1;point<4;point++)for(int axis=0;axis<3;axis++)signature[at++]=Math.round((basis[point*3+axis]-basis[axis])*100_000_000);
   for(float f:new float[]{mesh.minXyz.x,mesh.minXyz.y,mesh.minXyz.z,mesh.maxXyz.x,mesh.maxXyz.y,mesh.maxXyz.z})signature[at++]=Float.floatToIntBits(f);
  }
  var old=cache.get(item);if(old!=null&&old.meshes.equals(refs)&&Arrays.equals(old.basis,signature))return old.shape;
  var shape=read(frame,first,last,body);if(cache.size()>=128&&!cache.containsKey(item))cache.clear();cache.put(item,new Cached(List.copyOf(refs),signature,shape));return shape;
 }
 public static CollisionShape read(Frame frame,int first,int last,float[] body)throws ReflectiveOperationException{
  if(first<0||last<=first||last-first>16)throw new IllegalArgumentException("Storage item geometry range");
  if(meshesField==null){meshesField=ModelDraws.class.getDeclaredField("meshes");valuesField=ModelDraws.class.getDeclaredField("values");meshesField.setAccessible(true);valuesField.setAccessible(true);}
  var meshes=(ModelMesh[])meshesField.get(frame.scene.models);var values=(float[])valuesField.get(frame.scene.models);if(last>meshes.length||last>values.length/68)throw new IllegalArgumentException("Storage item packet extent");
  var parts=new ArrayList<CollisionShape>();int vertices=0,indices=0;
  for(int n=first;n<last;n++){
   var mesh=meshes[n];if(mesh==null||mesh.skinningData!=null)throw new IllegalArgumentException("Rigid storage geometry required");
   float[] v;int[] faces;var retained=ItemMeshCpu.get(mesh);var software=mesh.softwareMesh;
   if(retained!=null){v=retained.vertices();faces=retained.faces();}
   else if(software!=null&&software.verticesUnskinned!=null&&software.indicesUnskinned!=null){
    if(software.verticesUnskinned.length>32000||software.indicesUnskinned.length>192000)throw new IllegalArgumentException("Storage software geometry budget");
    v=new float[software.verticesUnskinned.length*3];faces=software.indicesUnskinned.clone();
    for(int i=0;i<software.verticesUnskinned.length;i++){var p=software.verticesUnskinned[i].position;v[i*3]=p.x();v[i*3+1]=p.y();v[i*3+2]=p.z();}
   }else throw new IllegalArgumentException("Original CPU storage item geometry unavailable");
   vertices+=v.length;indices+=faces.length;if(vertices>96000||indices>192000)throw new IllegalArgumentException("Combined storage geometry budget");
   parts.add(ItemMesh.fromRendered(v,faces,Arrays.copyOfRange(values,n*68,n*68+16),frame.camX,frame.camY,frame.camZ,body));
  }
  float[] v=new float[vertices];int[] faces=new int[indices];int at=0,face=0;
  for(var part:parts){var p=part.vertices();System.arraycopy(p,0,v,at,p.length);for(int index:part.faces())faces[face++]=at/3+index;at+=p.length;}
  return new CollisionShape(v,faces,new float[0],true);
 }
 private StorageItemGeometry(){}
}
