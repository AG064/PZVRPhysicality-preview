package pzvrphysicality;

import java.lang.reflect.Field;import java.util.List;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.VertexBufferObject;

/** Copies the one rigid imported mesh before its original GPU upload and source retirement. */
public final class UtensilMeshCpu {
 private record SceneLayout(Class<?> type,Field meshes,Field skinned){}
 private record MeshLayout(Class<?> type,Field vertices,Field elements){}
 private static volatile SceneLayout sceneLayout;private static volatile MeshLayout meshLayout;
 public static void capture(Object scene,ModelMesh target){
  if(target!=null)ItemMeshCpu.retire(target);
  if(!Main.ready()||scene==null||target==null||target.skinningData!=null)return;
  try{
   var layout=sceneLayout;if(layout==null||layout.type!=scene.getClass()){layout=new SceneLayout(scene.getClass(),field(scene.getClass(),"staticMeshes"),field(scene.getClass(),"skinnedMesh"));sceneLayout=layout;}
   if(layout.skinned.get(scene)!=null)return;Object value=layout.meshes.get(scene);
   if(!(value instanceof List<?> meshes)||meshes.size()!=1||meshes.getFirst()==null)return;var source=meshes.getFirst();
   var data=meshLayout;if(data==null||data.type!=source.getClass()){data=new MeshLayout(source.getClass(),field(source.getClass(),"verticesUnskinned"),field(source.getClass(),"elements"));meshLayout=data;}
   Object vertices=data.vertices.get(source),faces=data.elements.get(source);
   if(!(vertices instanceof VertexBufferObject.VertexArray array)||array.numVertices<4||array.numVertices>32000
    ||!(faces instanceof int[] indices)||indices.length<3||indices.length>192000||indices.length%3!=0)return;
   for(int index:indices)if(index<0||index>=array.numVertices)return;
   var positions=new float[array.numVertices*3];for(int n=0;n<array.numVertices;n++)for(int axis=0;axis<3;axis++)positions[n*3+axis]=array.getElementFloat(n,0,axis);
   ItemMeshCpu.offer(target,new ItemMeshCpu.Data(positions,indices));
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){/* Missing original geometry remains unavailable; native loading continues unchanged. */}
 }
 private static Field field(Class<?> type,String name)throws ReflectiveOperationException{var value=type.getDeclaredField(name);value.setAccessible(true);return value;}
 private UtensilMeshCpu(){}
}
