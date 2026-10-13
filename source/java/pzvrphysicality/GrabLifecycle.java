package pzvrphysicality;

/** Neutral-gated two-hand ownership. Native actions must confirm pickup before a release can drop it. */
public final class GrabLifecycle<T> {
 private static final class Side<T>{boolean ready,down;T pending,held;long expires;}
 private final Side<T> left=new Side<>(),right=new Side<>();
 private Side<T> side(int index){if(index<0||index>1)throw new IllegalArgumentException("Hand index");return index==0?left:right;}
 public enum Edge {NONE,GRAB,RELEASE}
 public Edge sample(int index,boolean down,boolean eligible,long now){
  var s=side(index);
  if(!eligible){s.ready=false;s.down=down;s.pending=null;return Edge.NONE;}
  if(s.pending!=null&&now>=s.expires)s.pending=null;
  if(!s.ready){if(!down){s.ready=true;s.down=false;}return Edge.NONE;}
  Edge edge=down==s.down?Edge.NONE:down?Edge.GRAB:Edge.RELEASE;s.down=down;return edge;
 }
 public boolean reserve(int index,T item,long now){var s=side(index);var other=side(1-index);if(item==null||!s.ready||!s.down||s.pending!=null||s.held!=null||other.pending==item||other.held==item)return false;s.pending=item;s.expires=now+5_000_000_000L;return true;}
 public boolean confirm(int index,T item,long now){var s=side(index);if(s.pending!=item||item==null||!s.ready||!s.down||now>=s.expires)return false;s.pending=null;s.held=item;return true;}
 public T held(int index){return side(index).held;}
 /** Native wear/attach actions can finish while physical floor input is deferred. Retire only absent held identities. */
 public void reconcile(T leftItem,T rightItem){for(var s:java.util.List.of(left,right))if(s.held!=null&&s.held!=leftItem&&s.held!=rightItem)s.held=null;}
 /** A separately guarded native body draw has already passed its own neutral edge and exact completion. */
 public boolean adopt(int index,T item,boolean grip){var s=side(index);var other=side(1-index);if(item==null||!grip||s.pending!=null||s.held!=null&&s.held!=item||other.pending==item||other.held==item)return false;s.held=item;s.ready=true;s.down=true;return true;}
 public boolean pending(int index,T item,long now){var s=side(index);return item!=null&&s.pending==item&&s.ready&&s.down&&now<s.expires;}
 public boolean hasPending(int index,long now){var s=side(index);return s.pending!=null&&s.ready&&s.down&&now<s.expires;}
 public void released(int index,T item){var s=side(index);if(s.held==item)s.held=null;}
 public void cancel(int index){side(index).pending=null;}
 public void reset(){for(var s:java.util.List.of(left,right)){s.ready=s.down=false;s.pending=s.held=null;s.expires=0;}}
}
