package pzvrphysicality;
public final class HeldContactDriver {
 public static boolean enabled,context=true,failFinish;public static int captures,finishes;public static boolean windowClosed,monitorsReleased;private static viewpoint.core.Frame captured;
 public static java.util.function.Consumer<HeldContactExternal.Offer> onOffer;public static java.util.List<HeldContactExternal.Offer> latest=java.util.List.of();
 public static boolean enabled(){return enabled;}public static boolean admits(viewpoint.core.Frame f,zombie.characters.IsoPlayer a,long n){return f!=null&&admitsScene(f.scene,a,n);}public static boolean admitsScene(Object s,zombie.characters.IsoPlayer a,long n){return enabled&&context&&a!=null&&!a.dead&&!a.timed&&a==zombie.characters.IsoPlayer.instance&&local.pzvr.Engine.latestInput!=null&&local.pzvr.Engine.latestInput.focused()&&WorldRigCapture.forScene(s,n)!=null;}
 public static void capture(viewpoint.core.Frame f){captures++;captured=f;latest=HeldContactExternal.targets(f,zombie.characters.IsoPlayer.instance,System.nanoTime());for(var offer:latest)if(onOffer!=null)onOffer.accept(offer);}
 public static void finishExternal(HeldContactExternal.Source source,Object scene){
  finishes++;if(Thread.holdsLock(NativeHeldFallback.class))throw new AssertionError("Native held mode monitor across driver finish");windowClosed=true;for(var offer:latest)if(offer.source()==source&&offer.scene()==scene&&source.current(offer,System.nanoTime()))windowClosed=false;
  if(captured!=null&&captured.scene==scene){boolean[] completed={false};var frame=captured;var task=new Thread(()->{source.targets(frame,zombie.characters.IsoPlayer.instance,System.nanoTime());completed[0]=true;});task.start();try{task.join(1000);}catch(InterruptedException failure){throw new AssertionError(failure);}monitorsReleased=completed[0];if(!monitorsReleased)throw new AssertionError("Source monitor across driver finish");}else monitorsReleased=true;
  latest=latest.stream().filter(offer->offer.source()!=source||offer.scene()!=scene).toList();if(failFinish)throw new IllegalStateException("authored cleanup failure");
 }
}
