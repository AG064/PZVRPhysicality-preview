package pzvrphysicality;
import java.util.List;
/** Numerical scene publication. Mesh and native water state are not borrowed or mutated. */
public record FaucetVisuals(long time,long generation,List<Row> rows){
 public record Row(ContactFrame.Point base,float cos,float sin,float scale,boolean open){}
 public FaucetVisuals{rows=List.copyOf(rows);if(rows.size()>8)throw new IllegalArgumentException("Faucet publication budget");}
 public boolean fresh(long now){return now>=time&&now-time<250_000_000L;}
 public static final FaucetVisuals EMPTY=new FaucetVisuals(0,0,List.of());
}
