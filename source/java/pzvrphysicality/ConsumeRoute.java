package pzvrphysicality;
/** Trigger is reserved only by the matching hand's live mouth interaction; all saved bindings stay intact. */
public final class ConsumeRoute {
 public static String resolve(boolean left,boolean right,int dominant,String key,String original){
  if(dominant<0||dominant>1)return original;
  int side=key.equals("dominant.trigger")?dominant:key.equals("other.trigger")?1-dominant:-1;
  return side==0&&left||side==1&&right?"none":original;
 }
 public static boolean suppressAuto(boolean enabled,boolean xr,boolean world,boolean localActor,boolean network){return enabled&&xr&&world&&localActor&&!network;}
 private ConsumeRoute(){}
}
