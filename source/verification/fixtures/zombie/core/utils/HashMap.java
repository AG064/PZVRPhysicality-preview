package zombie.core.utils;
/** Authored interpreter bookkeeping backed by standard Java collections. */
public final class HashMap {
 private final java.util.LinkedHashMap<Object,Object> values=new java.util.LinkedHashMap<>();
 public void clear(){values.clear();}public Object get(Object key){return values.get(key);}public Object put(Object key,Object value){return values.put(key,value);}public Object remove(Object key){return values.remove(key);}public int size(){return values.size();}public boolean isEmpty(){return values.isEmpty();}
 public Iterator iterator(){return new Iterator(this);}
 public static final class Iterator {
  private final HashMap owner;private java.util.Iterator<java.util.Map.Entry<Object,Object>> source;private java.util.Map.Entry<Object,Object> current;
  public Iterator(HashMap owner){this.owner=owner;reset();}
  public Iterator reset(){source=owner.values.entrySet().iterator();current=null;return this;}
  public boolean hasNext(){return source.hasNext();}public boolean advance(){if(!source.hasNext()){current=null;return false;}current=source.next();return true;}
  public Object getKey(){return current==null?null:current.getKey();}public Object getValue(){return current==null?null:current.getValue();}
 }
}
