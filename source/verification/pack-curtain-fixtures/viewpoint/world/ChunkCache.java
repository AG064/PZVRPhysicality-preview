package viewpoint.world;
public final class ChunkCache {public static final java.util.Map<zombie.iso.IsoGridSquare,viewpoint.render.ChunkMeshData> current=new java.util.IdentityHashMap<>();public static viewpoint.render.ChunkMeshData meshOn(zombie.iso.IsoGridSquare s){return current.get(s);}public static boolean packsOn(zombie.iso.IsoGridSquare s){return current.containsKey(s);}}
