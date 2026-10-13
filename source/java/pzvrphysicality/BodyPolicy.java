package pzvrphysicality;

/** Neutral gated drawing and release-to-store. Losing eligibility cannot replay a held grip. */
public final class BodyPolicy {
 public enum Edge {NONE,DRAW,STORE}
 private final boolean[] ready=new boolean[2],down=new boolean[2];
 public Edge sample(int side,boolean grip,boolean eligible){
  if(side<0||side>1)throw new IllegalArgumentException("Body hand");
  if(!eligible){ready[side]=false;down[side]=grip;return Edge.NONE;}
  if(!ready[side]){if(!grip){ready[side]=true;down[side]=false;}return Edge.NONE;}
  Edge edge=grip==down[side]?Edge.NONE:grip?Edge.DRAW:Edge.STORE;down[side]=grip;return edge;
 }
 public void reset(){java.util.Arrays.fill(ready,false);java.util.Arrays.fill(down,false);}
}
