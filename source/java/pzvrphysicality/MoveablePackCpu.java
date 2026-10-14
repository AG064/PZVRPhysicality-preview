package pzvrphysicality;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import viewpoint.render.PackData;
import viewpoint.render.PackModel;
import viewpointvr.physics.PackedModelGeometry;
import static org.lwjgl.opengl.GL33.*;

/** Immutable runtime position/normal/UV copies. Recovery reads only the exact current resident native pack slice. */
public final class MoveablePackCpu {
 public record Data(float[] vertices,int[] faces,long generation){
  public Data{if(vertices==null||faces==null||vertices.length<32||vertices.length%8!=0||vertices.length/8>32000||faces.length<3||faces.length%3!=0||faces.length>192000)throw new IllegalArgumentException("Moveable pack extent");vertices=vertices.clone();faces=faces.clone();
   for(int v=0;v<vertices.length;v+=8){for(int axis=0;axis<8;axis++)if(!Float.isFinite(vertices[v+axis])||Math.abs(vertices[v+axis])>4096)throw new IllegalArgumentException("Moveable pack numeric data");double n=vertices[v+3]*vertices[v+3]+vertices[v+4]*vertices[v+4]+vertices[v+5]*vertices[v+5];if(n<1e-8||n>16)throw new IllegalArgumentException("Moveable pack normal");}
   for(int index:faces)if(index<0||index>=vertices.length/8)throw new IllegalArgumentException("Moveable pack triangle");
  }
  public float[] vertices(){return vertices.clone();}public int[] faces(){return faces.clone();}
  public PackedModelGeometry.Mesh geometry(){var positions=new float[vertices.length/8*3];for(int v=0;v<positions.length/3;v++)System.arraycopy(vertices,v*8,positions,v*3,3);return new PackedModelGeometry.Mesh(positions,faces);}
 }
 private record Gpu(WeakReference<Object> page,int pageIndex,int vertexBuffer,int indexBuffer,int firstVertex,int vertices,int firstIndex,int indices){}
 private record Entry(Data data,Gpu gpu){}
 private record Attempt(long time,Gpu gpu,PackedModelGeometry.Mesh geometry){}
 private static final WeakHashMap<PackModel,Entry> cache=new WeakHashMap<>();private static final WeakHashMap<PackModel,Attempt> attempts=new WeakHashMap<>();private static long generation;
 private static Field pages,vertexBuffer,indexBuffer,meshPage,firstVertex,vertices,firstIndex,indices;
 public static synchronized void capture(PackModel model,PackData source){if(model==null)return;cache.remove(model);attempts.remove(model);if(source==null||PackData.VERTEX_FLOATS!=8)return;try{offer(model,new Data(source.vertices(),source.indices(),++generation),null);}catch(IllegalArgumentException unsupported){/* Native pack loading remains authoritative. */}}
 private static void offer(PackModel model,Data data,Gpu gpu){long total=cache.values().stream().mapToLong(e->(long)(e.data.vertices.length+e.data.faces.length)*4).sum();var old=cache.get(model);if(old!=null)total-=(long)(old.data.vertices.length+old.data.faces.length)*4;long bytes=(long)(data.vertices.length+data.faces.length)*4;if(cache.size()>=64&&!cache.containsKey(model)||total+bytes>16L*1024*1024)cache.clear();cache.put(model,new Entry(data,gpu));}
 private static boolean same(Data data,PackedModelGeometry.Mesh geometry){if(geometry==null)return false;var points=geometry.vertices();if(points.length!=data.vertices.length/8*3||!Arrays.equals(geometry.faces(),data.faces))return false;for(int v=0;v<points.length/3;v++)for(int axis=0;axis<3;axis++)if(Float.floatToIntBits(points[v*3+axis])!=Float.floatToIntBits(data.vertices[v*8+axis]))return false;return true;}
 public static synchronized Data get(PackModel model){var e=cache.get(model);if(e==null)return null;int state=model.state();if((state!=PackModel.READ&&state!=PackModel.RESIDENT)||!same(e.data,PackedModelGeometry.get(model))||e.gpu!=null&&!current(model,e.gpu)){cache.remove(model);return null;}return e.data;}
 public static synchronized void clear(){cache.clear();attempts.clear();generation++;}
 private static Field field(Class<?> type,String name)throws ReflectiveOperationException{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 private static Gpu snapshot(PackModel model)throws ReflectiveOperationException{
  if(pages==null){var arena=Class.forName("viewpoint.render.PackArena",false,MoveablePackCpu.class.getClassLoader());var page=Class.forName("viewpoint.render.PackArena$Page",false,MoveablePackCpu.class.getClassLoader());pages=field(arena,"pages");vertexBuffer=field(page,"vertices");indexBuffer=field(page,"indices");meshPage=field(PackModel.class,"meshPage");firstVertex=field(PackModel.class,"firstVertex");vertices=field(PackModel.class,"vertices");firstIndex=field(PackModel.class,"firstIndex");indices=field(PackModel.class,"indices");}
  int pageIndex=meshPage.getInt(model);var values=(List<?>)pages.get(null);if(pageIndex<0||pageIndex>=values.size())return null;var page=values.get(pageIndex);return page==null?null:new Gpu(new WeakReference<>(page),pageIndex,vertexBuffer.getInt(page),indexBuffer.getInt(page),firstVertex.getInt(model),vertices.getInt(model),firstIndex.getInt(model),indices.getInt(model));
 }
 private static boolean current(PackModel model,Gpu gpu){try{var now=snapshot(model);return now!=null&&model.state()==PackModel.RESIDENT&&now.page.get()==gpu.page.get()&&now.pageIndex==gpu.pageIndex&&now.vertexBuffer==gpu.vertexBuffer&&now.indexBuffer==gpu.indexBuffer&&now.firstVertex==gpu.firstVertex&&now.vertices==gpu.vertices&&now.firstIndex==gpu.firstIndex&&now.indices==gpu.indices;}catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){return false;}}
 public static boolean recover(PackModel model,long now){
  if(!Main.ready()||model==null||model.state()!=PackModel.RESIDENT)return false;var ready=get(model);if(ready!=null)return true;
  try{var gpu=snapshot(model);if(gpu==null||gpu.vertices<4||gpu.vertices>32000||gpu.indices<3||gpu.indices>192000||gpu.indices%3!=0||gpu.firstVertex<0||gpu.firstIndex<0)return false;
   var geometry=PackedModelGeometry.get(model);Entry expected;synchronized(MoveablePackCpu.class){expected=cache.get(model);var old=attempts.get(model);if(old!=null&&now>=old.time&&now-old.time<1_000_000_000L&&old.geometry==geometry&&equivalent(old.gpu,gpu))return false;if(attempts.size()>=64&&!attempts.containsKey(model))attempts.clear();attempts.put(model,new Attempt(now,gpu,geometry));}
   if(!glIsBuffer(gpu.vertexBuffer)||!glIsBuffer(gpu.indexBuffer)||gpu.vertexBuffer==gpu.indexBuffer)return false;var copied=new float[gpu.vertices*8];var faces=new int[gpu.indices];int before=glGetInteger(GL_COPY_READ_BUFFER);
   try{glBindBuffer(GL_COPY_READ_BUFFER,gpu.vertexBuffer);long vb=glGetBufferParameteri64(GL_COPY_READ_BUFFER,GL_BUFFER_SIZE),start=(long)gpu.firstVertex*32,extent=(long)copied.length*4;if(start<0||start+extent>vb)return false;glGetBufferSubData(GL_COPY_READ_BUFFER,start,copied);
    glBindBuffer(GL_COPY_READ_BUFFER,gpu.indexBuffer);long ib=glGetBufferParameteri64(GL_COPY_READ_BUFFER,GL_BUFFER_SIZE),offset=(long)gpu.firstIndex*4,bytes=(long)faces.length*4;if(offset<0||offset+bytes>ib)return false;glGetBufferSubData(GL_COPY_READ_BUFFER,offset,faces);
   }finally{glBindBuffer(GL_COPY_READ_BUFFER,before);}
   var candidate=new Data(copied,faces,0);if(!current(model,gpu)||PackedModelGeometry.get(model)!=geometry||geometry!=null&&!same(candidate,geometry))return false;
   PackedModelGeometry.Mesh linked;
   synchronized(PackedModelGeometry.class){if(PackedModelGeometry.get(model)!=geometry)return false;if(geometry==null)PackedModelGeometry.store(model,copied,faces,8);linked=PackedModelGeometry.get(model);if(!same(candidate,linked))return false;}
   synchronized(MoveablePackCpu.class){if(cache.get(model)!=expected||!current(model,gpu)||PackedModelGeometry.get(model)!=linked)return false;offer(model,new Data(copied,faces,++generation),gpu);return true;}
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){return false;}
 }
 private static boolean equivalent(Gpu a,Gpu b){return a!=null&&b!=null&&a.page.get()==b.page.get()&&a.pageIndex==b.pageIndex&&a.vertexBuffer==b.vertexBuffer&&a.indexBuffer==b.indexBuffer&&a.firstVertex==b.firstVertex&&a.vertices==b.vertices&&a.firstIndex==b.firstIndex&&a.indices==b.indices;}
 private MoveablePackCpu(){}
}
