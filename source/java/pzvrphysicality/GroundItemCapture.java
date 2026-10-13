package pzvrphysicality;

import java.lang.reflect.Field;
import java.util.*;
import viewpoint.core.Frame;
import viewpoint.render.ModelDraws;
import zombie.inventory.InventoryItem;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.core.skinnedmodel.model.ModelMesh;
import viewpointvr.physics.CollisionShape;
import viewpointvr.physics.RigidPhysics;

/** Numeric copies from the exact original item producer; never reads a GPU buffer or changes a render packet. */
public final class GroundItemCapture {
 private static final int STRIDE=68;
 private record Range(Frame frame,InventoryItem item,IsoWorldInventoryObject source,int first,long epoch,boolean skipGeometry){}
 private record Failed(java.lang.ref.WeakReference<Object> source,long epoch,long time){}
 private static final ThreadLocal<Range> range=new ThreadLocal<>();
 private static final ItemShapes<InventoryItem> shapes=new ItemShapes<>();
 private static final WeakHashMap<InventoryItem,Failed> failed=new WeakHashMap<>();
 private static Field meshField,valuesField;
 private static volatile long captured,unavailable;private static long warning;
 public static void begin(Frame frame,InventoryItem item){
  range.remove();if(!Main.ready()||!NativeItems.enabled()||frame==null||item==null||item.getWorldItem()==null)return;
  var source=item.getWorldItem();if(source.getItem()!=item||item.getContainer()!=null)return;
  var unavailable=failed.get(item);long now=System.nanoTime();
  boolean skip=unavailable!=null&&unavailable.source.get()==source&&unavailable.epoch==NativeItems.epoch()&&now>=unavailable.time&&now-unavailable.time<1_000_000_000L;
  range.set(new Range(frame,item,source,frame.scene.models.count(),NativeItems.epoch(),skip));
 }
 public static void end(Frame frame,InventoryItem item,boolean completed){
  var r=range.get();range.remove();
  if(r==null||!completed||r.frame!=frame||r.item!=item||!NativeItems.enabled()||r.epoch!=NativeItems.epoch()||item.getWorldItem()!=r.source||r.source.getItem()!=item||item.getContainer()!=null)return;
  try{
   if(r.skipGeometry)return;
   var sample=shapes.get(item,r.source,r.epoch,System.nanoTime());
   // Geometry is local to the native item pivot. Translation does not require a new mesh copy every frame.
   if(sample!=null&&System.nanoTime()-sample.time()<1_000_000_000L)return;
   var draws=frame.scene.models;int last=draws.count();
   if(last<=r.first||last-r.first>16)throw new IllegalArgumentException("Native item draw range");
   if(meshField==null){meshField=field("meshes");valuesField=field("values");}
   var meshes=(ModelMesh[])meshField.get(draws);var rows=(float[])valuesField.get(draws);
   if(r.first<0||last>meshes.length||last>rows.length/STRIDE)throw new IllegalArgumentException("Native item packet extent");
   var anchor=RigidPhysics.pose(r.source.getWorldPosX(),r.source.getWorldPosY(),(float)(r.source.getWorldPosZ()*Math.sqrt(6)));
   var parts=new ArrayList<CollisionShape>();int vertexCount=0,indexCount=0;
   for(int i=r.first;i<last;i++){
    var mesh=meshes[i];if(mesh==null||mesh.skinningData!=null)throw new IllegalArgumentException("Rigid item mesh unavailable");
    float[] positions;int[] triangles;var retained=ItemMeshCpu.get(mesh);var software=mesh.softwareMesh;
    if(retained!=null){positions=retained.vertices();triangles=retained.faces();}
    else if(software!=null&&software.verticesUnskinned!=null&&software.indicesUnskinned!=null){
     if(software.verticesUnskinned.length<4||software.verticesUnskinned.length>32000||software.indicesUnskinned.length<3||software.indicesUnskinned.length>192000)throw new IllegalArgumentException("Software item mesh extent");
     positions=new float[software.verticesUnskinned.length*3];triangles=software.indicesUnskinned.clone();
     for(int v=0;v<software.verticesUnskinned.length;v++){var value=software.verticesUnskinned[v];if(value==null||value.position==null)throw new IllegalArgumentException("Native item vertex missing");positions[v*3]=value.position.x();positions[v*3+1]=value.position.y();positions[v*3+2]=value.position.z();}
    }else throw new IllegalArgumentException("CPU rigid item mesh unavailable");
    int n=positions.length/3,indices=triangles.length;
    if(n<4||indices<3||indices%3!=0||(vertexCount+=n)>32000||(indexCount+=indices)>192000)throw new IllegalArgumentException("Native item geometry budget");
    var matrix=Arrays.copyOfRange(rows,i*STRIDE,i*STRIDE+16);
    parts.add(ItemMesh.fromRendered(positions,triangles,matrix,frame.camX,frame.camY,frame.camZ,anchor));
   }
   var vertices=new float[vertexCount*3];var faces=new int[indexCount];int v=0,f=0;
   for(var part:parts){var p=part.vertices();var indices=part.faces();System.arraycopy(p,0,vertices,v,p.length);for(int index:indices)faces[f++]=v/3+index;v+=p.length;}
   shapes.offer(item,r.source,new CollisionShape(vertices,faces,new float[0],true),r.epoch,System.nanoTime());failed.remove(item);captured++;
  }catch(ReflectiveOperationException|RuntimeException unsupported){unavailable++;long now=System.nanoTime();if(failed.size()>=128&&!failed.containsKey(item))failed.clear();failed.put(item,new Failed(new java.lang.ref.WeakReference<>(r.source),r.epoch,now));if(now-warning>5_000_000_000L){warning=now;System.err.println("[PZVR Physicality] Native item geometry unavailable: "+unsupported);}}
  finally{
   var pose=NativeItems.visualPose(item,r.source);
   if(pose!=null)try{DroppedItemPose.apply(frame,r.first,frame.scene.models.count(),new ContactFrame.Point(r.source.getWorldPosX(),r.source.getWorldPosY(),r.source.getWorldPosZ()*Math.sqrt(6)),pose);}
   catch(ReflectiveOperationException|RuntimeException unavailable){long now=System.nanoTime();if(now-warning>5_000_000_000L){warning=now;System.err.println("[PZVR Physicality] Dropped item pose unavailable: "+unavailable);}}
  }
 }
 public static CollisionShape shape(InventoryItem item,IsoWorldInventoryObject source,long epoch){var s=shapes.get(item,source,epoch,System.nanoTime());return s==null?null:s.shape();}
 public static void clear(){range.remove();shapes.clear();failed.clear();captured=unavailable=0;}
 public static String status(){return "native item mesh copies "+captured+", unavailable captures "+unavailable;}
 private static Field field(String name)throws ReflectiveOperationException{var f=ModelDraws.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private GroundItemCapture(){}
}
