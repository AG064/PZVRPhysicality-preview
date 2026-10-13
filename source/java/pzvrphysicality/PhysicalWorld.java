package pzvrphysicality;

import java.util.*;
import viewpointvr.physics.CollisionShape;
import viewpointvr.physics.RigidPhysics;

/** Reuses the verified Rust solver without any OpenXR, camera, animation or combat code. */
public final class PhysicalWorld<T> implements AutoCloseable {
 public record Static(String key,CollisionShape shape,float[] pose){
  public Static {if(key==null||key.isBlank()||key.length()>256||shape==null)throw new IllegalArgumentException("Static identity");pose=validPose(pose);}
  public float[] pose(){return pose.clone();}
 }
 private record Fixed(long handle,CollisionShape shape,float[] pose){}
 private final RigidPhysics solver=new RigidPhysics();
 private final Thread owner=Thread.currentThread();private boolean closed;
 private final IdentityHashMap<T,Long> bodies=new IdentityHashMap<>();
 private final IdentityHashMap<T,CollisionShape> shapes=new IdentityHashMap<>();
 private final Map<String,Fixed> statics=new HashMap<>();
 private final Set<T> held=Collections.newSetFromMap(new IdentityHashMap<>());
 private int preparing,failedGeometry;
 private static float[] validPose(float[] p){if(p==null||p.length!=7)throw new IllegalArgumentException("Physics pose extent");for(float x:p)if(!Float.isFinite(x))throw new IllegalArgumentException("Physics pose");return p.clone();}
 private void check(){if(Thread.currentThread()!=owner)throw new IllegalStateException("Physics owner thread changed");if(closed)throw new IllegalStateException("Physics world closed");}
 public void add(T key,CollisionShape shape,float[] pose,float mass){
  check();Objects.requireNonNull(key);Objects.requireNonNull(shape);pose=validPose(pose);
  if(bodies.containsKey(key))throw new IllegalStateException("Existing physical identity");
  if(bodies.size()>=72||!Float.isFinite(mass)||mass<=0||mass>100)throw new IllegalArgumentException("Body budget/mass");
  long body=shape.body(solver,1,pose,mass);bodies.put(key,body);shapes.put(key,shape);
 }
 /** Construct and validate a new collider before retiring the old one; retain position, rotation and velocity. */
 public boolean replace(T key,CollisionShape shape,float mass){
  check();Long old=bodies.get(key);if(old==null||shape==null||held.contains(key)||!Float.isFinite(mass)||mass<=0||mass>100)throw new IllegalArgumentException("Item collider replacement");
  if(shape==shapes.get(key)||shape.sameGeometry(shapes.get(key)))return false;
  float[] state=solver.state(old);var pose=validPose(Arrays.copyOf(state,7));long next=shape.body(solver,1,pose,mass);
  try{solver.velocity(next,Arrays.copyOfRange(state,7,13));}catch(RuntimeException|LinkageError failure){solver.remove(next);throw failure;}
  solver.remove(old);bodies.put(key,next);shapes.put(key,shape);return true;
 }
 public boolean environment(Collection<Static> geometry){
  check();if(geometry==null||geometry.size()>160)throw new IllegalArgumentException("Static contact budget");
  var unique=new HashSet<String>();for(var g:geometry)if(g==null||!unique.add(g.key))throw new IllegalArgumentException("Duplicate static identity");
  boolean changed=false;
  for(var iterator=statics.entrySet().iterator();iterator.hasNext();){var old=iterator.next();if(!unique.contains(old.getKey())){solver.remove(old.getValue().handle);iterator.remove();changed=true;}}
  for(var g:geometry){var old=statics.get(g.key);if(old!=null&&old.shape.sameGeometry(g.shape)&&CollisionShape.near(old.pose,g.pose))continue;
   long next=g.shape.body(solver,0,g.pose,1);if(old!=null)solver.remove(old.handle);statics.put(g.key,new Fixed(next,g.shape,g.pose.clone()));changed=true;
  }
  return changed;
 }
 public void wake(T key){check();Long handle=bodies.get(key);if(handle!=null){var state=solver.state(handle);solver.velocity(handle,Arrays.copyOfRange(state,7,13));}}
 public void sleep(T key){check();Long handle=bodies.get(key);if(handle==null)throw new IllegalArgumentException("Unknown sleeping item");solver.sleep(handle);}
 /** No placeholder collider and no synchronous decomposition on the game thread. */
 public boolean addPrepared(T key,CollisionShape shape,float[] pose,float mass){
  check();Objects.requireNonNull(key);Objects.requireNonNull(shape);pose=validPose(pose);
  if(bodies.containsKey(key))throw new IllegalStateException("Existing physical identity");
  if(bodies.size()>=72||shape.staticOnly()||!Float.isFinite(mass)||mass<=0||mass>100)throw new IllegalArgumentException("Prepared body budget/mass");
  if(shape.boxes().length>0){add(key,shape,pose,mass);return true;}
  int status=solver.prepareMesh(shape.vertices(),shape.faces());if(status!=1)return false;
  long body=solver.preparedMesh(pose,mass,shape.vertices(),shape.faces());if(body==0)return false;
  bodies.put(key,body);shapes.put(key,shape);return true;
 }
 /** Async gameplay path. A pending, failed or evicted preparation never removes the existing body. */
 public boolean prepareAndReplace(T key,CollisionShape shape,float mass){
  check();Long old=bodies.get(key);if(old==null||shape==null||shape.staticOnly()||held.contains(key)||!Float.isFinite(mass)||mass<=0||mass>100)throw new IllegalArgumentException("Prepared item collider replacement");
  if(shape==shapes.get(key)||shape.sameGeometry(shapes.get(key)))return false;
  if(shape.boxes().length>0)return replace(key,shape,mass);
  int status=solver.prepareMesh(shape.vertices(),shape.faces());if(status!=1){if(status<0)failedGeometry++;else preparing++;return false;}
  var state=solver.state(old);long next=solver.preparedMesh(validPose(Arrays.copyOf(state,7)),mass,shape.vertices(),shape.faces());if(next==0){preparing++;return false;}
  try{solver.velocity(next,Arrays.copyOfRange(state,7,13));}catch(RuntimeException|LinkageError error){solver.remove(next);throw error;}
  solver.remove(old);bodies.put(key,next);shapes.put(key,shape);return true;
 }
 public String preparationStatus(){return "collider waits "+preparing+", unavailable preparations "+failedGeometry;}
 public void hold(T key,float[] target,float mass){
  check();Long handle=bodies.get(key);target=validPose(target);
  if(handle==null||!Float.isFinite(mass)||mass<=0||mass>100)throw new IllegalArgumentException("Unknown held body/mass");
  solver.spring(handle,target,new float[]{Math.min(5000,mass*1000),Math.min(5000,mass*70),Math.min(500,mass*200),Math.min(5000,mass*80),Math.min(5000,mass*4),Math.min(50,mass*8)});held.add(key);
 }
 public void release(T key,float[] velocity){
  check();Long handle=bodies.get(key);if(handle==null)throw new IllegalArgumentException("Unknown drop body");
  if(velocity==null||velocity.length!=6)throw new IllegalArgumentException("Release velocity extent");
  for(float value:velocity)if(!Float.isFinite(value)||Math.abs(value)>50)throw new IllegalArgumentException("Release velocity budget");
  solver.releaseSpring(handle);held.remove(key);solver.velocity(handle,velocity.clone());
 }
 public void step(float seconds){check();if(!Float.isFinite(seconds)||seconds<=0||seconds>.05f)throw new IllegalArgumentException("Physics timestep");solver.step(seconds);}
 public float[] state(T key){check();Long handle=bodies.get(key);return handle==null?null:solver.state(handle);}
 public float distance(T key,float[] point){check();Long handle=bodies.get(key);return handle==null?Float.POSITIVE_INFINITY:solver.distance(handle,point);}
 public void remove(T key){check();Long handle=bodies.get(key);if(handle!=null)solver.remove(handle);bodies.remove(key);shapes.remove(key);held.remove(key);}
 public int count(){check();return bodies.size();}
 public int staticCount(){check();return statics.size();}
 public boolean held(T key){check();return held.contains(key);}
 public void close(){if(closed)return;check();solver.close();closed=true;bodies.clear();shapes.clear();statics.clear();held.clear();}
}
