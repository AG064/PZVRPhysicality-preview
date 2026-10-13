package pzvrphysicality;

/** One semantic bite. The original food stays in native inventory and is never split or modified here. */
public final class UtensilPolicy<T> {
 public record Bite<T>(int side,T tool,T food,double hunger,double baseHunger,double portion,long loaded){}
 private static final class Hand<T>{T tool,food;boolean away;long contact=-1;}
 private final Hand<T> left=new Hand<>(),right=new Hand<>();
 private Bite<T> bite;private long activeTicket;private boolean leftFood;
 private Hand<T> hand(int side){if(side<0||side>1)throw new IllegalArgumentException("Utensil hand");return side==0?left:right;}
 private static boolean quantity(double hunger,double base){return Double.isFinite(hunger)&&Double.isFinite(base)&&hunger<0&&base<0;}
 public Bite<T> sample(int side,T tool,T food,double hunger,double base,boolean touchingFood,boolean touchingMouth,boolean eligible,boolean busy,long now){
  var h=hand(side);
  if(h.tool!=tool||h.food!=food){h.tool=tool;h.food=food;h.away=false;h.contact=-1;if(bite!=null&&bite.side==side)clearBite();}
  if(!eligible||tool==null||food==null||tool==food||!quantity(hunger,base)){
   h.away=false;h.contact=-1;if(bite!=null&&bite.side==side)clearBite();return null;
  }
  if(bite!=null&&bite.side==side){
   if(bite.tool!=tool||bite.food!=food||Double.doubleToLongBits(bite.hunger)!=Double.doubleToLongBits(hunger)
    ||Double.doubleToLongBits(bite.baseHunger)!=Double.doubleToLongBits(base)||now<bite.loaded||now-bite.loaded>30_000_000_000L){
    clearBite();h.away=false;h.contact=-1;return null;
   }
   if(!touchingFood)leftFood=true;
   return bite;
  }
  if(!touchingFood){h.away=true;h.contact=-1;return null;}
  if(!h.away||touchingMouth||busy||bite!=null){h.contact=-1;return null;}
  if(h.contact<0||now<h.contact){h.contact=now;return null;}
  if(now-h.contact<150_000_000L)return null;
  double portion=Math.min(MouthMath.biteFraction(base),Math.min(1,hunger/base));
  if(!Double.isFinite(portion)||portion<=0)return null;
  bite=new Bite<>(side,tool,food,hunger,base,portion,now);activeTicket=0;leftFood=false;h.away=false;h.contact=-1;
  return bite;
 }
 public Bite<T> bite(){return bite;}
 public long ticket(){return activeTicket;}
 public boolean reserved(T food){return bite!=null&&bite.food==food;}
 public boolean ready(int side,T tool,T food){return bite!=null&&bite.side==side&&bite.tool==tool&&bite.food==food&&leftFood;}
 public boolean bind(long ticket,int side,T food){if(ticket<=0||activeTicket!=0||bite==null||bite.side!=side||bite.food!=food||!leftFood)return false;activeTicket=ticket;return true;}
 public boolean owns(long ticket,int side,T tool,T food){return activeTicket==ticket&&ticket>0&&ready(side,tool,food);}
 public void finish(long ticket){if(activeTicket==ticket&&ticket>0)clearBite();}
 public void cancel(int side){var h=hand(side);h.away=false;h.contact=-1;if(bite!=null&&bite.side==side)clearBite();}
 private void clearBite(){bite=null;activeTicket=0;leftFood=false;}
 public void reset(){clearBite();for(var h:java.util.List.of(left,right)){h.tool=h.food=null;h.away=false;h.contact=-1;}}
}
