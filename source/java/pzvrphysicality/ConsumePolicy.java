package pzvrphysicality;

/** One native action at a time. It reserves identity, never food, liquid, inventory or nutrition values. */
public final class ConsumePolicy<T> {
 public enum Kind{FOOD,DRINK,UTENSIL}
 public record Request<T>(long ticket,int side,T item,Kind kind,long created){}
 private static final class Hand<T>{T item;boolean ready,away=true;long contactSince=-1;}
 private final Hand<T> left=new Hand<>(),right=new Hand<>();private long sequence;private Request<T> pending,active;private long retryAt,activeSince;
 private Hand<T> hand(int side){if(side<0||side>1)throw new IllegalArgumentException("Hand");return side==0?left:right;}
 public Request<T> sample(int side,T item,Kind kind,boolean near,boolean flow,boolean trigger,boolean eligible,boolean busy,boolean requireTrigger,long now){
  var h=hand(side);
  if(pending!=null&&now-pending.created>500_000_000L)pending=null;
  if(h.item!=item){h.item=item;h.ready=false;h.away=true;h.contactSince=-1;}
  if(!eligible||item==null||kind==null){if(pending!=null&&pending.side==side)pending=null;h.ready=false;h.contactSince=-1;return null;}
  boolean deliberate=!requireTrigger||trigger;
  if(!near||!deliberate){h.away=true;h.contactSince=-1;if(!trigger)h.ready=true;return null;}
  // Enabling while already in the mouth cannot immediately consume an item.
  if(!h.ready){h.contactSince=-1;return null;}
  if(pending!=null||active!=null||busy||now<retryAt||kind!=Kind.DRINK&&!h.away||kind==Kind.DRINK&&!flow){h.contactSince=-1;return null;}
  if(h.contactSince<0){h.contactSince=now;return null;}
  if(now-h.contactSince<200_000_000L)return null;
  pending=new Request<>(++sequence,side,item,kind,now);h.contactSince=-1;return pending;
 }
 public Request<T> request(){return pending;}
 public boolean busy(){return pending!=null||active!=null;}
 public boolean acknowledge(long ticket,boolean queued,long now){
  if(pending==null||pending.ticket!=ticket||now<pending.created||now-pending.created>500_000_000L)return false;
  if(queued){active=pending;activeSince=now;hand(pending.side).away=false;}else retryAt=now+300_000_000L;
  pending=null;return true;
 }
 public boolean owns(long ticket,int side,T item){return active!=null&&active.ticket==ticket&&active.side==side&&active.item==item;}
 public void finish(long ticket,long now){if(active!=null&&active.ticket==ticket){active=null;retryAt=now+250_000_000L;}}
 public void reconcile(boolean nativeBusy,long now){if(active!=null&&!nativeBusy&&now-activeSince>250_000_000L){active=null;retryAt=now+250_000_000L;}}
 public void reset(){pending=active=null;retryAt=0;for(var h:java.util.List.of(left,right)){h.item=null;h.ready=false;h.away=true;h.contactSince=-1;}}
 private ConsumePolicy(int unused){}
 public ConsumePolicy(){this(0);}
}
