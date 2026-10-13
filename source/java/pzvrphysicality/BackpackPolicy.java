package pzvrphysicality;

/** Neutral grip edges only. Native actions own all inventory and quantity effects. */
public final class BackpackPolicy {
 public enum Edge {NONE,PICK,RETURN}
 private final boolean[] neutral=new boolean[2],previous=new boolean[2];
 public Edge sample(int side,boolean grip,boolean tracked){if(side<0||side>1)throw new IllegalArgumentException("Backpack hand");if(!tracked){neutral[side]=false;previous[side]=grip;return Edge.NONE;}if(!neutral[side]){if(!grip)neutral[side]=true;previous[side]=grip;return Edge.NONE;}var result=grip&&!previous[side]?Edge.PICK:!grip&&previous[side]?Edge.RETURN:Edge.NONE;previous[side]=grip;return result;}
 /** Only an exact current native pickup receipt can restore the already observed held-down lineage. */
 public boolean adopt(int side,boolean grip){if(side<0||side>1||!grip)return false;neutral[side]=previous[side]=true;return true;}
 public void reset(){java.util.Arrays.fill(neutral,false);java.util.Arrays.fill(previous,false);}
}
