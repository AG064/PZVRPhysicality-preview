package pzvrphysicality;
import java.util.*;

/** Sleeping rigid bodies retain solver contacts but do not force per-frame native object updates. */
public final class ItemRestBudget<T> {
 private record State(boolean sleeping,long time,float[] pose){}
 private final IdentityHashMap<T,State> states=new IdentityHashMap<>();
 public boolean known(T key){return states.containsKey(key);}
 public boolean awake(){for(var state:states.values())if(!state.sleeping)return true;return false;}
 public boolean due(T key,long now){var s=states.get(key);return s==null||!s.sleeping||now<s.time||now-s.time>=100_000_000L;}
 public void sample(T key,float[] state,long now){
  Objects.requireNonNull(key);if(state==null||state.length!=14)throw new IllegalArgumentException("Rigid state extent");
  for(float x:state)if(!Float.isFinite(x))throw new IllegalArgumentException("Rigid state number");
  states.put(key,new State(state[13]==1,now,Arrays.copyOf(state,7)));
 }
 public float[] pose(T key){var s=states.get(key);return s==null?null:s.pose.clone();}
 public void wake(T key){states.remove(key);}
 public void remove(T key){states.remove(key);}
 public void clear(){states.clear();}
}
