package pzvrphysicality;

/** Controller estimates remain distinct from optical joints or squeeze contact. */
public final class FingerPose {
 public record Curl(boolean available,float value){public static final Curl UNKNOWN=new Curl(false,0);public Curl{if(!available||!Float.isFinite(value)){available=false;value=0;}else value=Math.max(0,Math.min(1,value));}}
 public record Pose(Curl thumb,Curl index,Curl grip){}
 /** Small analog fluctuations only; discrete contact and deliberate changes stay immediate. */
 public static final class AnalogFilter {
  static final long GAP=250_000_000L;static final double CONSTANT=20_000_000.0;static final float SMALL_CHANGE=.12f;
  private long time=Long.MIN_VALUE;private float filtered,raw;
  public void reset(){time=Long.MIN_VALUE;}
  public Curl sample(ControllerTouch.Value value,long sampleTime){
   if(value==null||!value.available()||sampleTime<=0){reset();return Curl.UNKNOWN;}
   float next=value.value();
   if(time==sampleTime&&next==raw)return new Curl(true,filtered);
   if(time==Long.MIN_VALUE||sampleTime<=time||sampleTime-time>GAP||Math.abs(next-filtered)>SMALL_CHANGE)filtered=next;
   else filtered+=(next-filtered)*(float)(1-Math.exp(-(sampleTime-time)/CONSTANT));
   raw=next;time=sampleTime;return new Curl(true,filtered);
  }
 }
 public static Pose resolve(ControllerTouch.Input input){
  if(input==null)input=ControllerTouch.Input.UNKNOWN;
  var thumb=thumb(input.stick(),input.rest(),input.primary(),input.secondary());
  Curl index=input.triggerValue().available()?new Curl(true,input.triggerValue().value()):Curl.UNKNOWN;
  if(input.trigger().available())index=new Curl(true,Math.max(index.available()?index.value():0,input.trigger().touched()?.18f:0));
  var grip=input.squeeze().available()?new Curl(true,input.squeeze().value()):Curl.UNKNOWN;
  return new Pose(thumb,index,grip);
 }
 private static Curl thumb(ControllerTouch.Contact... contacts){boolean all=true;for(var contact:contacts){if(contact.available()&&contact.touched())return new Curl(true,.65f);all&=contact.available();}return all?new Curl(true,0):Curl.UNKNOWN;}
 /** The stock body exposes one grouped non-thumb root. Preserve unknown grip. */
 public static Curl grouped(Pose pose){return pose.grip().available()?new Curl(true,Math.max(pose.grip().value(),pose.index().available()?pose.index().value():0)):Curl.UNKNOWN;}
 private FingerPose(){}
}
