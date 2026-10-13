package viewpointvr.physics;
import java.util.*;
import java.lang.reflect.*;

/** Copies canonical packed geometry at the native CPU-read boundary, before GPU upload discards it. */
public final class PackedModelGeometry {
 public record Mesh(float[] vertices,int[] faces){public Mesh{vertices=vertices.clone();faces=faces.clone();}public float[] vertices(){return vertices.clone();}public int[] faces(){return faces.clone();}}
 private static final class Entry {final Mesh mesh;long time;Entry(Mesh mesh,long time){this.mesh=mesh;this.time=time;}}
 private static final Map<Object,Entry> cache=new WeakHashMap<>();private static long warning;
 private PackedModelGeometry(){}
 public static void capture(Object model,Object data){try{
  var type=data.getClass();int stride=type.getField("VERTEX_FLOATS").getInt(null);if(stride<3||stride>16)throw new IllegalArgumentException("Packed vertex layout");
  store(model,(float[])type.getMethod("vertices").invoke(data),(int[])type.getMethod("indices").invoke(data),stride);
 }catch(ReflectiveOperationException|RuntimeException error){long now=System.nanoTime();if(now-warning>5_000_000_000L){warning=now;System.err.println("[Project Viewpoint VR Packed Geometry] Geometry skipped: "+error);}}}
 public static synchronized void store(Object model,float[] vertices,int[] faces,int stride){
  if(model==null)return;
  if(vertices==null||faces==null||stride<3||stride>16||vertices.length%stride!=0||vertices.length/stride>32000||vertices.length/stride<4||faces.length<3||faces.length%3!=0||faces.length>192000){cache.remove(model);return;}
  var positions=new float[vertices.length/stride*3];for(int i=0;i<positions.length/3;i++)for(int axis=0;axis<3;axis++){float value=vertices[i*stride+axis];if(!Float.isFinite(value)||Math.abs(value)>32){cache.remove(model);return;}positions[i*3+axis]=value;}
  for(int index:faces)if(index<0||index>=positions.length/3){cache.remove(model);return;}var mesh=new Mesh(positions,faces);long size=(long)(positions.length+faces.length)*4;cache.remove(model);
  // Weak keys can disappear between writes. Count the actual retained entries,
  // rather than keeping a counter that still charges collected models.
  long bytes=cache.values().stream().mapToLong(value->(long)(value.mesh.vertices.length+value.mesh.faces.length)*4).sum();
  while(cache.size()>=256||bytes+size>32L*1024*1024){var oldest=cache.entrySet().stream().min(Comparator.comparingLong(value->value.getValue().time)).orElse(null);if(oldest==null)break;bytes-=(long)(oldest.getValue().mesh.vertices.length+oldest.getValue().mesh.faces.length)*4;cache.remove(oldest.getKey());}
  cache.put(model,new Entry(mesh,System.nanoTime()));
 }
 public static synchronized Mesh get(Object model){var value=cache.get(model);if(value==null)return null;value.time=System.nanoTime();return value.mesh;}
 /** A normal native reload wins over an older background recovery. */
 public static synchronized boolean recover(Object model,float[] vertices,int[] faces,int stride){if(cache.containsKey(model))return true;store(model,vertices,faces,stride);return cache.containsKey(model);}
}
