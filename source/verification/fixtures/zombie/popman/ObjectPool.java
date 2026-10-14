package zombie.popman;
/** Small authored pool; no game statistics, logging or global Lua manager. */
public class ObjectPool<T> {
 @FunctionalInterface public interface Allocator<T>{T allocate();}
 private final Allocator<T> allocator;private final java.util.ArrayDeque<T> pool=new java.util.ArrayDeque<>();private int maximum;
 public ObjectPool(String name){this(null,name,1024);}
 public ObjectPool(Allocator<T> allocator,String name){this(allocator,name,1024);}
 public ObjectPool(Allocator<T> allocator,String name,int maximum){this.allocator=allocator;setMaxSize(maximum);}
 public synchronized void setMaxSize(int value){maximum=Math.max(0,value);while(pool.size()>maximum)pool.removeLast();}
 protected T makeObject(){if(allocator==null)throw new IllegalStateException("Authored allocator required");return allocator.allocate();}
 public synchronized T alloc(){return pool.isEmpty()?makeObject():pool.removeFirst();}
 public synchronized void release(T value){if(value!=null&&pool.size()<maximum&&!pool.contains(value))pool.addFirst(value);}
 public void release(java.util.List<T> values){values.forEach(this::release);}public void release(Iterable<T> values){values.forEach(this::release);}public void release(T[] values){for(T value:values)release(value);}
 public void releaseAll(java.util.List<T> values){release(values);}public void releaseAll(java.util.Collection<T> values){values.forEach(this::release);}
 public synchronized void clear(){pool.clear();}public synchronized int size(){return pool.size();}public synchronized void forEach(java.util.function.Consumer<T> action){pool.forEach(action);}
}
