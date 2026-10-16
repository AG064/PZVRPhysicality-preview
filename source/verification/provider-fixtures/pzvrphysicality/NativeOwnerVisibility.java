package pzvrphysicality;
public final class NativeOwnerVisibility {
 public static boolean demand,installed,visible=true,watch=true;public static Object recipe;public static viewpoint.render.SceneData scene;
 public static final viewpoint.render.ChunkMeshData data=new viewpoint.render.ChunkMeshData();
 public static boolean request(Object token,boolean enabled){demand=enabled;return demand;}
 public static boolean watchRecipe(Object value,zombie.iso.IsoGridSquare square){if(!watch||!demand)return false;recipe=value;return true;}
 public static viewpoint.render.ChunkMeshData committed(Object value,zombie.iso.IsoGridSquare square){return demand&&installed&&value==recipe?data:null;}
 public static boolean contents(viewpoint.render.SceneData value,viewpoint.render.ChunkMeshData mesh,zombie.iso.IsoGridSquare square){return visible&&value==scene&&mesh==data;}
}
