package pzvrphysicality;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import viewpointvr.physics.CollisionShape;

/** Bounded numeric geometry cache. Neither keys nor their native world sources are retained strongly. */
public final class ItemShapes<T> {
 public record Sample(CollisionShape shape,long time,long epoch){}
 private record Entry(CollisionShape shape,long time,long epoch,WeakReference<Object> source){}
 private final WeakHashMap<T,Entry> entries=new WeakHashMap<>();
 public synchronized boolean offer(T item,Object source,CollisionShape shape,long epoch,long now){
  if(item==null||source==null||shape==null||!shape.exact()||shape.staticOnly()||epoch<0)throw new IllegalArgumentException("Item geometry publication");
  var old=entries.get(item);
  if(old!=null&&old.epoch==epoch&&old.source.get()==source&&old.shape.sameGeometry(shape)){entries.put(item,new Entry(old.shape,now,epoch,old.source));return false;}
  if(entries.size()>=128&&!entries.containsKey(item))entries.clear();
  entries.put(item,new Entry(shape,now,epoch,new WeakReference<>(source)));return true;
 }
 public synchronized Sample get(T item,Object source,long epoch,long now){
  var e=entries.get(item);return e!=null&&source!=null&&e.source.get()==source&&e.epoch==epoch&&now>=e.time&&now-e.time<30_000_000_000L?new Sample(e.shape,e.time,e.epoch):null;
 }
 public synchronized void clear(){entries.clear();}
 public synchronized int size(){return entries.size();}
}
