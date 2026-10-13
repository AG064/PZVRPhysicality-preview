package pzvrphysicality;

import java.util.WeakHashMap;

/** Trigger and wrist turn on an identified handle. No cursor, gaze, source quantities or inventory writes. */
public final class TapGesture<T> {
 public enum Change{NONE,OPENED,CLOSED}
 private final WeakHashMap<T,Boolean> valves=new WeakHashMap<>();
 private final Object[] selected=new Object[2];private final boolean[] ready=new boolean[2],latched=new boolean[2];private final double[] initial=new double[2];
 public Change sample(int side,T source,boolean touching,boolean trigger,double angleDegrees,boolean eligible){
  if(side<0||side>1)throw new IllegalArgumentException("Tap hand");
  if(!eligible||!Double.isFinite(angleDegrees)){selected[side]=null;ready[side]=false;latched[side]=false;return Change.NONE;}
  if(!trigger){ready[side]=true;latched[side]=false;selected[side]=null;return Change.NONE;}
  if(!ready[side]||source==null||!touching||latched[side]){if(!touching)selected[side]=null;return Change.NONE;}
  if(selected[side]!=source){selected[side]=source;initial[side]=angleDegrees;return Change.NONE;}
  double delta=(angleDegrees-initial[side])%360;if(delta>180)delta-=360;if(delta< -180)delta+=360;
  if(Math.abs(delta)<35)return Change.NONE;
  if(valves.size()>=64&&!valves.containsKey(source))return Change.NONE;
  boolean open=!Boolean.TRUE.equals(valves.get(source));valves.put(source,open);latched[side]=true;
  return open?Change.OPENED:Change.CLOSED;
 }
 public boolean open(T source){return Boolean.TRUE.equals(valves.get(source));}
 public void reset(){valves.clear();java.util.Arrays.fill(selected,null);java.util.Arrays.fill(ready,false);java.util.Arrays.fill(latched,false);}
 public static boolean atStream(boolean open,ContactFrame.Point spout,ContactFrame.Point target,boolean filling,double unitsPerMetre){
  if(!open||spout==null||target==null||!Double.isFinite(unitsPerMetre)||unitsPerMetre<.25||unitsPerMetre>4)return false;
  double dx=(target.x()-spout.x())/unitsPerMetre,dy=(target.y()-spout.y())/unitsPerMetre,down=(spout.height()-target.height())/unitsPerMetre;
  return dx*dx+dy*dy<.08*.08&&down>=-.015&&down<=(filling?.22:.09);
 }
}
