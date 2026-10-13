package pzvrphysicality;

import java.lang.reflect.Field;import java.util.*;
import zombie.core.skinnedmodel.model.ModelMesh;import zombie.core.skinnedmodel.model.VertexBufferObject;

/** Bounded weak copies of original UVs and normals, retained only with the matching CPU geometry generation. */
public final class BiteMeshCpu {
 private record Gpu(java.lang.ref.WeakReference<Object> vbo,java.lang.ref.WeakReference<Object> handle,java.lang.ref.WeakReference<Object> format,int revision,int vertices,int elements,int stride,int count,int position,int normal,int uv){}
 private record Data(BiteGeometry.Mesh mesh,ItemMeshCpu.Data geometry,int bytes,Gpu gpu,String signature){}
 public record Snapshot(BiteGeometry.Mesh mesh,ItemMeshCpu.Data geometry,String signature){}
 private static final WeakHashMap<ModelMesh,Data> cache=new WeakHashMap<>();
 private record Attempt(java.lang.ref.WeakReference<Object> vbo,int vertices,int elements,int stride,int count,ItemMeshCpu.Data cpu,int revision,long time){}
 private static final WeakHashMap<ModelMesh,Attempt> attempts=new WeakHashMap<>();
 public static void capture(Object scene,ModelMesh target){
  synchronized(BiteMeshCpu.class){cache.remove(target);}if(!Main.ready()||scene==null||target==null||target.skinningData!=null)return;
  try{
   if(field(scene,"skinnedMesh").get(scene)!=null)return;Object raw=field(scene,"staticMeshes").get(scene);if(!(raw instanceof List<?> meshes)||meshes.size()!=1||meshes.getFirst()==null)return;
   Object imported=meshes.getFirst();Object vertices=field(imported,"verticesUnskinned").get(imported),elements=field(imported,"elements").get(imported);
   if(vertices instanceof VertexBufferObject.VertexArray array&&elements instanceof int[] faces)offer(target,array,faces);
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){/* The optional visual retains native food and eating behavior. */}
 }
 static synchronized void offer(ModelMesh target,VertexBufferObject.VertexArray source,int[] faces){
  var geometry=ItemMeshCpu.get(target);if(geometry==null||source==null||source.format==null||source.numVertices<3||source.numVertices>4096||faces==null||faces.length>12288)return;
  int p=source.format.indexOf(VertexBufferObject.VertexType.VertexArray),n=source.format.indexOf(VertexBufferObject.VertexType.NormalArray),uv=source.format.indexOf(VertexBufferObject.VertexType.TextureCoordArray);
  if(p<0||n<0||uv<0||source.format.getElement(p).byteSize<12||source.format.getElement(n).byteSize<12||source.format.getElement(uv).byteSize<8)return;
  var copied=new float[source.numVertices*8];var positions=geometry.vertices();if(positions.length!=source.numVertices*3||!Arrays.equals(faces,geometry.faces()))return;
  for(int v=0;v<source.numVertices;v++){
   for(int axis=0;axis<3;axis++){float value=source.getElementFloat(v,p,axis);if(Float.floatToIntBits(value)!=Float.floatToIntBits(positions[v*3+axis]))return;copied[v*8+axis]=value;copied[v*8+3+axis]=source.getElementFloat(v,n,axis);}
   copied[v*8+6]=source.getElementFloat(v,uv,0);copied[v*8+7]=source.getElementFloat(v,uv,1);
  }
  var mesh=new BiteGeometry.Mesh(copied,faces);int bytes=copied.length*4+faces.length*4;int total=cache.values().stream().mapToInt(Data::bytes).sum();var old=cache.get(target);if(old!=null)total-=old.bytes;
  if(cache.size()>=64&&!cache.containsKey(target)||total+bytes>2*1024*1024)cache.clear();cache.put(target,new Data(mesh,geometry,bytes,null,signature(mesh)));
 }
 public static synchronized BiteGeometry.Mesh get(ModelMesh mesh){var data=cache.get(mesh);if(data==null)return null;if(ItemMeshCpu.get(mesh)!=data.geometry||data.gpu!=null&&!current(mesh,data.gpu)){cache.remove(mesh);return null;}return data.mesh;}
 public static synchronized Snapshot snapshot(ModelMesh mesh){if(get(mesh)==null)return null;var data=cache.get(mesh);return new Snapshot(data.mesh,data.geometry,data.signature);}
 private static String signature(BiteGeometry.Mesh mesh){
  try{float[] vertices=mesh.vertices();int[] faces=mesh.faces();var bytes=java.nio.ByteBuffer.allocate((vertices.length+faces.length)*4).order(java.nio.ByteOrder.LITTLE_ENDIAN);for(float value:vertices)bytes.putInt(Float.floatToIntBits(value));for(int value:faces)bytes.putInt(value);return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.array()));}
  catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
 }
 private static boolean current(ModelMesh mesh,Gpu gpu){
  try{
   Object vbo=gpu.vbo.get(),handle=gpu.handle.get(),format=gpu.format.get();if(vbo==null||handle==null||format==null||mesh.vb!=vbo||mesh.modificationCount!=gpu.revision||field(vbo,"handle").get(vbo)!=handle||field(vbo,"vertexFormat").get(vbo)!=format)return false;
   var h=(VertexBufferObject.Vbo)handle;var f=(VertexBufferObject.VertexFormat)format;int p=f.indexOf(VertexBufferObject.VertexType.VertexArray),n=f.indexOf(VertexBufferObject.VertexType.NormalArray),uv=f.indexOf(VertexBufferObject.VertexType.TextureCoordArray);
   return p>=0&&n>=0&&uv>=0&&h.vboId==gpu.vertices&&h.eboId==gpu.elements&&h.vertexStride==gpu.stride&&h.numElements==gpu.count&&f.getStride()==gpu.stride&&f.getElement(p).byteSize>=12&&f.getElement(n).byteSize>=12&&f.getElement(uv).byteSize>=8&&f.getElement(p).byteOffset==gpu.position&&f.getElement(n).byteOffset==gpu.normal&&f.getElement(uv).byteOffset==gpu.uv;
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){return false;}
 }
 public static synchronized boolean recover(ModelMesh mesh,long now){
  if(!Main.ready()||mesh==null||!mesh.isReady()||mesh.skinningData!=null||mesh.vb==null)return false;
  try{
   Object vbo=mesh.vb;var handle=(VertexBufferObject.Vbo)field(vbo,"handle").get(vbo);var format=(VertexBufferObject.VertexFormat)field(vbo,"vertexFormat").get(vbo);
   if(handle==null||format==null||format.getStride()!=handle.vertexStride)return false;
   int revision=mesh.modificationCount,vertices=handle.vboId,elements=handle.eboId,stride=handle.vertexStride,count=handle.numElements;var cpu=ItemMeshCpu.get(mesh);
   var old=attempts.get(mesh);if(old!=null&&old.vbo.get()==vbo&&old.vertices==vertices&&old.elements==elements&&old.stride==stride&&old.count==count&&old.cpu==cpu&&old.revision==revision&&now>=old.time&&now-old.time<1_000_000_000L)return get(mesh)!=null;
   if(attempts.size()>=64&&!attempts.containsKey(mesh))attempts.clear();attempts.put(mesh,new Attempt(new java.lang.ref.WeakReference<>(vbo),vertices,elements,stride,count,cpu,revision,now));
   int p=format.indexOf(VertexBufferObject.VertexType.VertexArray),n=format.indexOf(VertexBufferObject.VertexType.NormalArray),uv=format.indexOf(VertexBufferObject.VertexType.TextureCoordArray);
   if(p<0||n<0||uv<0||format.getElement(p).byteSize<12||format.getElement(n).byteSize<12||format.getElement(uv).byteSize<8)return false;
   int position=format.getElement(p).byteOffset,normal=format.getElement(n).byteOffset,texture=format.getElement(uv).byteOffset;
   var recovered=BiteGpuReadback.read(vertices,elements,stride,position,normal,texture,count);
   if(!mesh.isReady())return false;
   if(mesh.vb!=vbo||mesh.modificationCount!=revision||mesh.skinningData!=null||field(vbo,"handle").get(vbo)!=handle||field(vbo,"vertexFormat").get(vbo)!=format||handle.vboId!=vertices||handle.eboId!=elements||handle.vertexStride!=stride||handle.numElements!=count||format.getStride()!=stride||format.getElement(p).byteOffset!=position||format.getElement(n).byteOffset!=normal||format.getElement(uv).byteOffset!=texture||ItemMeshCpu.get(mesh)!=cpu)return false;
   if(cpu!=null&&(!Arrays.equals(cpu.vertices(),recovered.positions())||!Arrays.equals(cpu.faces(),recovered.faces())))return false;
   var geometry=cpu!=null?cpu:new ItemMeshCpu.Data(recovered.positions(),recovered.faces());if(cpu==null&&!ItemMeshCpu.offerIfUnchanged(mesh,null,geometry))return false;var result=recovered.mesh();int bytes=result.vertices().length*4+result.faces().length*4;
   var stamp=new Gpu(new java.lang.ref.WeakReference<>(vbo),new java.lang.ref.WeakReference<>(handle),new java.lang.ref.WeakReference<>(format),revision,vertices,elements,stride,count,position,normal,texture);
   if(cache.size()>=64&&!cache.containsKey(mesh)||cache.values().stream().mapToInt(Data::bytes).sum()+bytes>2*1024*1024)cache.clear();cache.put(mesh,new Data(result,geometry,bytes,stamp,signature(result)));return true;
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){return false;}
 }
 private static Field field(Object object,String name)throws ReflectiveOperationException{var field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field;}
 private BiteMeshCpu(){}
}
