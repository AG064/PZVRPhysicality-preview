package zombie.iso;
public final class IsoChunk {public int wx,wy;public final java.util.Map<String,IsoGridSquare> squares=new java.util.HashMap<>();public IsoChunk(int x,int y){wx=x;wy=y;}public IsoGridSquare getGridSquare(int x,int y,int z){return squares.get(x+":"+y+":"+z);}}
