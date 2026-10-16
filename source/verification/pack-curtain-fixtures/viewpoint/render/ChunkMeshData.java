package viewpoint.render;
public final class ChunkMeshData {public int level,arenaFirst;public int[] runs,first,count;public zombie.core.textures.TextureID[] pages;private boolean retired;public ChunkMeshData(int z){level=z;}public void retire(){retired=true;}}
