package pzvrphysicality;
import java.util.*;

/** Stable bounded slots. A missing model never removes the corresponding native inventory item. */
public final class BackpackLayout<T> {
 public static final int PAGE_SIZE=8,MAX_ITEMS=2048;
 private final List<T> slots=new ArrayList<>();
 public void reconcile(List<T> items){if(items==null||items.size()>MAX_ITEMS)throw new IllegalArgumentException("Backpack inventory budget");var present=Collections.newSetFromMap(new IdentityHashMap<T,Boolean>());for(var item:items)if(item==null||!present.add(item))throw new IllegalArgumentException("Backpack duplicate inventory identity");var assigned=Collections.newSetFromMap(new IdentityHashMap<T,Boolean>());var free=new ArrayDeque<Integer>();for(int i=0;i<slots.size();i++){var item=slots.get(i);if(item==null||!present.contains(item)){slots.set(i,null);free.add(i);}else assigned.add(item);}for(var item:items)if(!assigned.contains(item)){if(free.isEmpty())slots.add(item);else slots.set(free.removeFirst(),item);assigned.add(item);}while(!slots.isEmpty()&&slots.getLast()==null)slots.removeLast();}
 public int pages(){return Math.max(1,Math.min(MAX_ITEMS/PAGE_SIZE,(slots.size()+PAGE_SIZE)/PAGE_SIZE));}
 public int count(){int count=0;for(var item:slots)if(item!=null)count++;return count;}
 public T at(int page,int cell){if(page<0||page>=pages()||cell<0||cell>=PAGE_SIZE)throw new IllegalArgumentException("Backpack page");int index=page*PAGE_SIZE+cell;return index<slots.size()?slots.get(index):null;}
 public boolean place(T item,int page,int cell){if(item==null||page<0||cell<0||cell>=PAGE_SIZE||page*PAGE_SIZE+cell>=MAX_ITEMS)return false;int target=page*PAGE_SIZE+cell;while(slots.size()<=target)slots.add(null);var old=slots.get(target);if(old!=null&&old!=item)return false;for(int i=0;i<slots.size();i++)if(slots.get(i)==item)slots.set(i,null);slots.set(target,item);return true;}
 public void clear(){slots.clear();}
 /** Fixed cells have room for real small items; oversized models remain in the original inventory UI. */
 public static MouthMath.Vec cell(int cell,double bagTop){if(cell<0||cell>=PAGE_SIZE||!Double.isFinite(bagTop))throw new IllegalArgumentException("Backpack cell");return new MouthMath.Vec((cell%4-1.5)*.20,bagTop+.06,-.10-(cell/4)*.24);}
 public static MouthMath.Vec cell(int cell,MouthMath.Vec mount){if(mount==null)throw new IllegalArgumentException("Backpack mesh mount");var offset=cell(cell,mount.y());return new MouthMath.Vec(mount.x()+offset.x(),offset.y(),mount.z()+offset.z());}
}
