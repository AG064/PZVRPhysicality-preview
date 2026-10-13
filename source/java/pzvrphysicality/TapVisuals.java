package pzvrphysicality;
import java.util.List;
/** Immutable numeric render publication. It does not retain inventory, model, frame or GPU resources. */
public record TapVisuals(long time,long generation,List<Dot> dots,List<Stream> streams){
 public record Dot(ContactFrame.Point point,boolean hovered){}
 public record Stream(ContactFrame.Point start,ContactFrame.Point end,double units){}
 public TapVisuals{dots=List.copyOf(dots);streams=List.copyOf(streams);if(dots.size()>32||streams.size()>8)throw new IllegalArgumentException("Tap visual budget");}
 public boolean fresh(long now){return now>=time&&now-time<250_000_000L;}
 public static final TapVisuals EMPTY=new TapVisuals(0,0,List.of(),List.of());
}
