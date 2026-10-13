package pzvrphysicality;

import java.lang.reflect.Field;
import java.util.*;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.VertexBufferObject;

/** Copies rigid TXT mesh positions before GPU upload. Bounded to 16 MiB and weak mesh identities. */
public final class ItemMeshCpu {
 public record Data(float[] vertices,int[] faces){
  public Data{if(vertices==null||faces==null||vertices.length<12||vertices.length>96000||vertices.length%3!=0||faces.length<3||faces.length>192000||faces.length%3!=0)throw new IllegalArgumentException("CPU item mesh extent");vertices=vertices.clone();faces=faces.clone();
   for(float v:vertices)if(!Float.isFinite(v))throw new IllegalArgumentException("CPU item vertex");for(int f:faces)if(f<0||f>=vertices.length/3)throw new IllegalArgumentException("CPU item face");}
  public float[] vertices(){return vertices.clone();}public int[] faces(){return faces.clone();}
  int bytes(){return (vertices.length+faces.length)*4;}
 }
 private static final WeakHashMap<ModelMesh,Data> cache=new WeakHashMap<>();
 private record Layout(Class<?> type,Field isStatic,Field vertices,Field elements){}
 private static volatile Layout access;
 public static void capture(Object loaded,ModelMesh mesh){
  if(!Main.ready()||loaded==null||mesh==null)return;
  try{
   var layout=access;if(layout==null||layout.type!=loaded.getClass()){layout=new Layout(loaded.getClass(),field(loaded,"isStatic"),field(loaded,"vertices"),field(loaded,"elements"));access=layout;}
   if(!layout.isStatic.getBoolean(loaded))return;
   var source=(VertexBufferObject.VertexArray)layout.vertices.get(loaded);var indices=(int[])layout.elements.get(loaded);
   if(source==null||source.numVertices<4||source.numVertices>32000||indices==null||indices.length<3||indices.length>192000)return;
   var data=new float[source.numVertices*3];for(int v=0;v<source.numVertices;v++)for(int c=0;c<3;c++)data[v*3+c]=source.getElementFloat(v,0,c);
   offer(mesh,new Data(data,indices));
  }catch(ReflectiveOperationException|RuntimeException invalid){/* A missing rigid source remains an explicitly reported fallback at the item producer. */}
 }
 public static synchronized void offer(ModelMesh mesh,Data data){
  Objects.requireNonNull(mesh);Objects.requireNonNull(data);int bytes=cache.values().stream().mapToInt(Data::bytes).sum();var old=cache.get(mesh);if(old!=null)bytes-=old.bytes();
  if(cache.size()>=256&&!cache.containsKey(mesh)||bytes+data.bytes()>16*1024*1024)cache.clear();cache.put(mesh,data);
 }
 public static synchronized Data get(ModelMesh mesh){return cache.get(mesh);}
 public static synchronized boolean offerIfUnchanged(ModelMesh mesh,Data expected,Data data){if(cache.get(mesh)!=expected)return false;offer(mesh,data);return true;}
 public static synchronized void retire(ModelMesh mesh){if(mesh!=null)cache.remove(mesh);}
 private static Field field(Object object,String name)throws ReflectiveOperationException{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f;}
 private ItemMeshCpu(){}
}
