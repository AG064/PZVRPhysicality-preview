package pzvrphysicality;

/** One deliberate native-window intent; recipes, ingredients and outputs remain native. */
public final class CraftingPolicy<T> {
 public record Request<T>(long ticket,int side,T tool,long created){}
 private static final class Hand<T>{T tool;boolean neutral,away;long contact=-1;}
 private final Hand<T> left=new Hand<>(),right=new Hand<>();private Request<T> pending;private long sequence,retryAt;
 private Hand<T> hand(int side){if(side<0||side>1)throw new IllegalArgumentException("Crafting hand");return side==0?left:right;}
 public Request<T> sample(int side,T tool,boolean touching,boolean trigger,boolean eligible,boolean busy,long now){
  var h=hand(side);if(pending!=null&&(now<pending.created||now-pending.created>500_000_000L)){var old=hand(pending.side);old.neutral=old.away=false;old.contact=-1;pending=null;retryAt=now+500_000_000L;}
  if(h.tool!=tool){h.tool=tool;h.neutral=h.away=false;h.contact=-1;if(pending!=null&&pending.side==side)pending=null;}
  if(!eligible||tool==null){h.neutral=h.away=false;h.contact=-1;if(pending!=null&&pending.side==side)pending=null;return null;}
  if(!trigger)h.neutral=true;if(!touching)h.away=true;
  if(!touching||!trigger||!h.neutral||!h.away||busy||pending!=null||now<retryAt){h.contact=-1;return null;}
  if(h.contact<0||now<h.contact){h.contact=now;return null;}if(now-h.contact<250_000_000L)return null;
  pending=new Request<>(++sequence,side,tool,now);h.contact=-1;return pending;
 }
 public Request<T> request(){return pending;}
 public boolean valid(long ticket,int side,T tool,long now){return pending!=null&&pending.ticket==ticket&&pending.side==side&&pending.tool==tool&&now>=pending.created&&now-pending.created<=500_000_000L;}
 public boolean acknowledge(long ticket,boolean opened,long now){if(pending==null||pending.ticket!=ticket)return false;var h=hand(pending.side);boolean fresh=now>=pending.created&&now-pending.created<=500_000_000L;pending=null;h.neutral=h.away=false;h.contact=-1;retryAt=now+500_000_000L;return !opened||fresh;}
 public void reset(){pending=null;retryAt=0;for(var h:java.util.List.of(left,right)){h.tool=null;h.neutral=h.away=false;h.contact=-1;}}
}
