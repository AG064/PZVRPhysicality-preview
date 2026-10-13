package pzvrphysicality;
import java.util.*;
import viewpointvr.physics.CollisionShape;

/** Detached shape/pose simulation; native inventory ownership stays entirely in the caller. */
public final class StorageBodies<T> implements AutoCloseable {
 public record Sample(float[] state,long revision){public Sample{state=state.clone();}public float[] state(){return state.clone();}}
 private record Pending(CollisionShape shape,float[] pose,float mass,long revision,boolean sleeping){}
 private final PhysicalWorld<T> world=new PhysicalWorld<>();private final IdentityHashMap<T,Long> revisions=new IdentityHashMap<>();
 private final IdentityHashMap<T,Pending> pending=new IdentityHashMap<>();private final ItemRestBudget<T> rest=new ItemRestBudget<>();
 private final ArrayDeque<T> queue=new ArrayDeque<>();private long lastStep;private double accumulator;private boolean closed;
 private final IdentityHashMap<T,float[]> states=new IdentityHashMap<>();
 private final Thread owner=Thread.currentThread();private void check(){if(Thread.currentThread()!=owner)throw new IllegalStateException("Storage world owner changed");if(closed)throw new IllegalStateException("Storage world closed");}
 public boolean offer(T key,CollisionShape shape,float[] pose,float mass,long revision,boolean saved){
  check();if(key==null||shape==null||pose==null||pose.length!=7||revision<1||revision>2147483646)throw new IllegalArgumentException("Storage body identity");
  for(float v:pose)if(!Float.isFinite(v))throw new IllegalArgumentException("Storage body pose");
  if(!Float.isFinite(mass)||mass<=0||mass>100||shape.staticOnly())throw new IllegalArgumentException("Storage body shape/mass");
  if(revisions.containsKey(key)&&revisions.get(key)>=revision){if(world.prepareAndReplace(key,shape,mass))rest.wake(key);return true;}
  if(revisions.containsKey(key)){world.remove(key);revisions.remove(key);rest.remove(key);}
  if(revisions.size()+pending.size()>=64&&!pending.containsKey(key))return false;
  if(!pending.containsKey(key))queue.addLast(key);pending.put(key,new Pending(shape,pose.clone(),mass,revision,saved));return false;
 }
 public void environment(Collection<PhysicalWorld.Static> values){
  check();
  if(world.environment(values)){for(var key:revisions.keySet())world.wake(key);rest.clear();}
 }
 public void tick(long now){
  check();int attempts=0;
  int available=queue.size();while(attempts<Math.min(4,available)){
   var key=queue.removeFirst();var p=pending.get(key);attempts++;if(p==null)continue;
   if(!world.addPrepared(key,p.shape,p.pose,p.mass)){queue.addLast(key);continue;}
   if(p.sleeping)world.sleep(key);revisions.put(key,p.revision);var state=world.state(key);states.put(key,state);rest.sample(key,state,now);pending.remove(key);
  }
  boolean awake=rest.awake()||revisions.keySet().stream().anyMatch(key->!rest.known(key));
  if(awake&&lastStep!=0&&now>lastStep){accumulator=Math.min(.1,accumulator+(now-lastStep)/1e9);while(accumulator>=1.0/120){world.step(1f/120);accumulator-=1.0/120;}}else if(!awake)accumulator=0;lastStep=now;
  for(var key:revisions.keySet())if(rest.due(key,now)){var state=world.state(key);if(state!=null){states.put(key,state);rest.sample(key,state,now);}}
 }
 public Sample sample(T key){check();var revision=revisions.get(key);if(revision==null)return null;var state=states.get(key);return state==null?null:new Sample(state,revision);}
 public void revision(T key,long expected,long next){check();if(revisions.getOrDefault(key,0L)==expected&&next>=expected&&next<=2147483646)revisions.put(key,next);}
 public void released(T key,float[] velocity){check();if(revisions.containsKey(key)){world.release(key,velocity);rest.wake(key);}}
 public void retain(Collection<T> keys){check();var retained=Collections.newSetFromMap(new IdentityHashMap<T,Boolean>());retained.addAll(keys);
  for(var iterator=revisions.keySet().iterator();iterator.hasNext();){var key=iterator.next();if(!retained.contains(key)){world.remove(key);rest.remove(key);states.remove(key);iterator.remove();}}
  pending.keySet().removeIf(key->!retained.contains(key));
  queue.removeIf(key->!pending.containsKey(key));
 }
 public void remove(T key){check();pending.remove(key);queue.removeIf(value->value==key);revisions.remove(key);rest.remove(key);states.remove(key);world.remove(key);}
 public int count(){check();return revisions.size();}public int pending(){check();return pending.size();}
 public void close(){if(closed)return;check();world.close();closed=true;revisions.clear();pending.clear();queue.clear();rest.clear();states.clear();}
}
