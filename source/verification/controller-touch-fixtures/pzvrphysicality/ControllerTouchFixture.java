package pzvrphysicality;
import java.util.*;import local.pzvr.ControllerProfiles;import local.pzvr.XrBackend;import org.lwjgl.system.MemoryStack;
/** Entirely authored transport. No OpenXR native functions are called. */
public final class ControllerTouchFixture implements ControllerTouch.Transport {
 public int creates,suggestions,contacts,values;public int result;public boolean failCreate,failQuery;public long leftProfile=101,rightProfile=101;
 public List<ControllerTouch.Bound> union=List.of();public final Map<String,ControllerTouch.Contact> touch=new HashMap<>();public final Map<String,ControllerTouch.Value> analog=new HashMap<>();public final Map<Object,long[]> declared=new HashMap<>();
 public Object create(ControllerTouch.Context c,ControllerTouch.Channel channel,MemoryStack stack){creates++;if(failCreate)return null;declared.put(channel,c.hands.clone());return channel;}
 private void query(ControllerTouch.Context c,Object action,int side){if(!Arrays.equals(declared.get(action),c.hands)||side<0||side>1||c.hands[side]==0)throw new AssertionError("Declared action/query subaction path mismatch");}
 public int suggest(ControllerTouch.Context c,ControllerProfiles.Profile profile,List<ControllerTouch.Bound> bindings,MemoryStack stack){suggestions++;union=bindings;return result;}
 public long path(ControllerTouch.Context c,String text,MemoryStack stack){return ControllerTouch.PROFILE.equals(text)?101:102;}
 public long profile(ControllerTouch.Context c,int side,MemoryStack stack){return side==0?leftProfile:rightProfile;}
 public ControllerTouch.Contact contact(ControllerTouch.Context c,Object action,int side,MemoryStack stack){query(c,action,side);contacts++;if(failQuery)throw new IllegalStateException("authored unavailable");return touch.getOrDefault(action+"/"+side,ControllerTouch.Contact.UNKNOWN);}
 public ControllerTouch.Value value(ControllerTouch.Context c,Object action,int side,MemoryStack stack){query(c,action,side);values++;return analog.getOrDefault(action+"/"+side,ControllerTouch.Value.UNKNOWN);}
 public void contact(ControllerTouch.Channel channel,int side,boolean touched){touch.put(channel+"/"+side,new ControllerTouch.Contact(true,touched));}
 public void value(ControllerTouch.Channel channel,int side,float value){analog.put(channel+"/"+side,new ControllerTouch.Value(true,value));}
 public void supported(int side){for(var channel:ControllerTouch.Channel.values())if(channel.analog)value(channel,side,0);else contact(channel,side,false);}
 public static ControllerTouch.Context context(Object backend,Object session,Object set,Object lock,boolean eligible){return new ControllerTouch.Context(backend,new Object(),session,set,lock,new long[]{11,12},Map.of("native_trigger",new Object(),"native_grip",new Object()),eligible);}
 public static ControllerProfiles.Profile profile(){return new ControllerProfiles.Profile(ControllerTouch.PROFILE,"Touch","",List.of(new ControllerProfiles.Binding("native_trigger","/user/hand/right/input/trigger/value"),new ControllerProfiles.Binding("native_grip","/user/hand/left/input/squeeze/value")));}
 public static XrBackend.Hand hand(boolean tracked){var pose=tracked?new local.pzvr.Math3.Pose(new local.pzvr.Math3.Vec(0,0,0),new local.pzvr.Math3.Quat(0,0,0,1)):null;return new XrBackend.Hand(pose,pose,tracked,0,0,0);}
 public static XrBackend.Frame frame(long time,XrBackend.Hand left,XrBackend.Hand right){return new XrBackend.Frame(time,true,true,null,List.of(),left,right);}
}
