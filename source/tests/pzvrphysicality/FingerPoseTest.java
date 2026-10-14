package pzvrphysicality;
/** Pure channel policy with no game, native transport or pose callbacks. */
public final class FingerPoseTest {
 private static int checks;private static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);checks++;}
 private static ControllerTouch.Input input(ControllerTouch.Contact trigger,ControllerTouch.Contact stick,ControllerTouch.Contact rest,ControllerTouch.Contact primary,ControllerTouch.Contact secondary,ControllerTouch.Value triggerValue,ControllerTouch.Value squeeze){return new ControllerTouch.Input(trigger,stick,rest,primary,secondary,triggerValue,squeeze);}
 public static void main(String[] args){
  var u=ControllerTouch.Contact.UNKNOWN;var off=new ControllerTouch.Contact(true,false);var on=new ControllerTouch.Contact(true,true);var v=ControllerTouch.Value.UNKNOWN;
  var unknown=FingerPose.resolve(null);check(!unknown.thumb().available()&&!unknown.index().available()&&!unknown.grip().available(),"no sensors means unknown");
  var open=FingerPose.resolve(input(off,off,off,off,off,new ControllerTouch.Value(true,0),new ControllerTouch.Value(true,0)));check(open.thumb().available()&&open.thumb().value()==0,"known untouched thumb opens");check(open.index().value()==0&&open.grip().value()==0,"open analog values remain independent");
  for(int n=0;n<4;n++){var contacts=new ControllerTouch.Contact[]{off,off,off,off};contacts[n]=on;var pose=FingerPose.resolve(input(off,contacts[0],contacts[1],contacts[2],contacts[3],v,v));check(pose.thumb().available()&&pose.thumb().value()>.5,"each thumb contact can rest thumb");check(!pose.grip().available(),"thumb touch never fabricates grip");}
  var partial=FingerPose.resolve(input(u,off,u,off,off,v,v));check(!partial.thumb().available(),"missing rest is unknown even when others untouched");
  var touched=FingerPose.resolve(input(on,u,u,u,u,v,v));check(touched.index().available()&&touched.index().value()==.18f,"trigger contact rests index");check(!touched.thumb().available()&&!touched.grip().available(),"index touch is separate");
  var squeeze=FingerPose.resolve(input(off,off,off,off,off,new ControllerTouch.Value(true,.2f),new ControllerTouch.Value(true,.8f)));check(squeeze.index().value()==.2f&&squeeze.grip().value()==.8f,"trigger and squeeze analog distinct");check(FingerPose.grouped(squeeze).value()==.8f,"stock grouped root respects stronger grip");
  var pointing=FingerPose.resolve(input(off,off,off,off,off,new ControllerTouch.Value(true,.9f),new ControllerTouch.Value(true,.1f)));check(FingerPose.grouped(pointing).value()==.9f,"stock grouped root cannot split index and remaining fingers");
  check(!FingerPose.grouped(touched).available(),"unknown squeeze retains native grouped pose");
  for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,-.1f,1.1f})check(!new ControllerTouch.Value(true,bad).available(),"invalid analog unknown");
  check(!new ControllerTouch.Contact(false,true).touched(),"unavailable cannot mean touched");check(!new FingerPose.Curl(true,Float.NaN).available(),"nonfinite curl unknown");check(new FingerPose.Curl(true,2).value()==1,"curl upper bound");check(new FingerPose.Curl(true,-1).value()==0,"curl lower bound");
  var filter=new FingerPose.AnalogFilter();long time=1_000_000_000L;check(filter.sample(new ControllerTouch.Value(true,.48f),time).value()==.48f,"first analog sample stays immediate");
  float smooth=filter.sample(new ControllerTouch.Value(true,.52f),time+8_333_333L).value();check(smooth>.48f&&smooth<.52f,"small analog fluctuation filtered");check(filter.sample(new ControllerTouch.Value(true,.52f),time+8_333_333L).value()==smooth,"same sample does not advance filter per eye/capture");
  check(filter.sample(new ControllerTouch.Value(true,1),time+16_666_666L).value()==1,"large closure change immediate");check(filter.sample(new ControllerTouch.Value(true,0),time+25_000_000L).value()==0,"large opening change immediate");
  check(!filter.sample(v,time+30_000_000L).available(),"unavailable analog resets filter");check(filter.sample(new ControllerTouch.Value(true,.7f),time+40_000_000L).value()==.7f,"recovery starts at real pressure");
  check(filter.sample(new ControllerTouch.Value(true,.75f),time+500_000_000L).value()==.75f,"long sample gap resets filter");check(filter.sample(new ControllerTouch.Value(true,.8f),time+490_000_000L).value()==.8f,"clock rollback starts fresh");filter.reset();check(filter.sample(new ControllerTouch.Value(true,.4f),time+510_000_000L).value()==.4f,"explicit epoch reset starts fresh");
  double rawJump=0,filteredJump=0;float previousRaw=0,previousFiltered=0;filter.reset();
  for(int n=0;n<120;n++){float pressure=n%2==0?.48f:.52f;float output=filter.sample(new ControllerTouch.Value(true,pressure),time+n*8_333_333L).value();if(n>20){rawJump+=Math.abs(pressure-previousRaw);filteredJump+=Math.abs(output-previousFiltered);}previousRaw=pressure;previousFiltered=output;}
  check(filteredJump<rawJump*.25,"authored120Hz jitter reduced by at least75percent after settling");
  var once=new FingerPose.AnalogFilter();var split=new FingerPose.AnalogFilter();once.sample(new ControllerTouch.Value(true,.4f),time);split.sample(new ControllerTouch.Value(true,.4f),time);split.sample(new ControllerTouch.Value(true,.5f),time+10_000_000L);check(Math.abs(once.sample(new ControllerTouch.Value(true,.5f),time+20_000_000L).value()-split.sample(new ControllerTouch.Value(true,.5f),time+20_000_000L).value())<.000001f,"small analog filtering follows sample time instead of frame count");
  System.out.printf(java.util.Locale.ROOT,"Authored analog continuity: raw mean step %.9f, filtered %.9f, reduction %.3f percent%n",rawJump/99,filteredJump/99,100*(1-filteredJump/rawJump));
  System.out.println("Finger pose policy: "+checks+" checks passed; controller estimates only");
 }
}
