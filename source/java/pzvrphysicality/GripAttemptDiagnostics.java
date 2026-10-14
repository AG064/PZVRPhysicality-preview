package pzvrphysicality;
import java.util.ArrayDeque;import local.pzvr.Engine;import local.pzvr.XrBackend;import zombie.characters.IsoPlayer;

/** Bounded observation of the sole upstream input publication; never creates an input or samples contacts. */
public final class GripAttemptDiagnostics {
 private record Attempt(long number,long at,int side,String detail){}
 private static final ArrayDeque<Attempt> attempts=new ArrayDeque<>();private static boolean enabled;private static Object actor,cell;private static long time=-1,serial;private static int previous,pressed;private static XrBackend.Frame pending;private static String before;
 private static void edges(){time=-1;previous=pressed=0;pending=null;before=null;}
 public static synchronized void configure(boolean value){if(enabled!=value)edges();enabled=value;}
 public static synchronized void reset(){attempts.clear();actor=cell=null;time=-1;previous=pressed=0;pending=null;before=null;serial=0;}
 private static String bound(String value,int maximum){return value==null?"?":value.replace('\n',' ').replace('\r',' ').substring(0,Math.min(maximum,value.length()));}
 private static IsoPlayer player(){var players=IsoPlayer.players;return players==null||players.length==0?null:players[0];}
 private static int buttons(XrBackend.Frame input){int value=0;for(int side=0;side<2;side++){var hand=side==0?input.left():input.right();if(hand!=null&&hand.down(XrBackend.GRIP))value|=1<<side;}return value;}
 private static String sample(){var request=NativeItems.request();String guard=NativeItems.guardStatus();String phase=guard.contains("HUD")||guard.contains("pointer")||guard.contains("modal")?"UI-owned":guard.contains("paused")?"paused":"world/guard";return "phase="+phase+" request="+(request==null?"none":request.kind()+"/side"+request.side())+" item-status="+bound(NativeItems.status(),110)+" guard="+bound(guard,120)+" contacts="+bound(PzvrContacts.status(),90)+" rig="+bound(WorldRigCapture.status(),100)+" hands="+bound(NativeHandState.status(NativeItems.gestureBusy(0),NativeItems.gestureBusy(1)),400);}
 public static synchronized void begin(boolean ready){
  pressed=0;pending=null;before=null;if(!enabled)return;
  try{
   var p=player();if(p==null||p.getCell()==null){reset();return;}
   if(actor!=p||cell!=p.getCell()){reset();actor=p;cell=p.getCell();}
   if(!ready||!Engine.worldPresent){edges();return;}
   var input=Engine.latestInput;if(input==null||input.time()<0){time=-1;previous=0;return;}
   int current=buttons(input);
   if(time<0||input.time()<time){time=input.time();previous=current;return;}
   if(input.time()==time)return;
   time=input.time();pressed=current&~previous;previous=current;if(pressed==0)return;
   pending=input;before=bound(sample(),650);
  }catch(RuntimeException|LinkageError unavailable){pressed=0;pending=null;before=null;}
 }
 public static synchronized void end(){
  if(!enabled||pressed==0||pending==null)return;
  try{
   var p=player();if(p!=actor||p==null||p.getCell()!=cell){reset();return;}if(!Engine.worldPresent)return;
   String after=bound(sample(),900);long now=System.nanoTime();
   for(int side=0;side<2;side++)if((pressed&(1<<side))!=0){var hand=side==0?pending.left():pending.right();var current=Engine.latestInput;String detail="raw-focused="+pending.focused()+" raw-tracked="+(hand!=null&&hand.tracked())+" input-time="+pending.time()+" publication-replaced="+(current!=pending)+" before={"+before+"} after={"+after+"}";if(attempts.size()==8)attempts.removeFirst();attempts.addLast(new Attempt(++serial,now,side,detail));}
  }catch(RuntimeException|LinkageError unavailable){/* Observation failures cannot change an interaction. */}
  finally{pressed=0;pending=null;before=null;}
 }
 public static synchronized String status(){
  if(!enabled)return "Grip attempt receipts Off";if(attempts.isEmpty())return "No raw grip press edge observed in this world; release, then press grip";
  StringBuilder value=new StringBuilder("Last "+attempts.size()+" raw grip attempts (newest first); ");long now=System.nanoTime();var iterator=attempts.descendingIterator();while(iterator.hasNext()){var attempt=iterator.next();value.append('#').append(attempt.number()).append(attempt.side()==0?" Left":" Right").append(" ageMs=").append(Math.max(0,now-attempt.at())/1_000_000L).append(' ').append(attempt.detail()).append(" | ");}return value.toString();
 }
 private GripAttemptDiagnostics(){}
}
