package pzvrphysicality;

import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;
import local.pzvr.ControllerProfiles;
import java.util.List;
import static org.lwjgl.openxr.XR10.*;

/** Uses the already-created instance, session and action set only. */
final class ControllerTouchXr implements ControllerTouch.Transport {
 public Object create(ControllerTouch.Context c,ControllerTouch.Channel channel,MemoryStack stack){
  var pointer=stack.mallocPointer(1);var info=XrActionCreateInfo.calloc(stack).type$Default().actionName(stack.UTF8(channel.action())).localizedActionName(stack.UTF8(channel.action())).actionType(channel.analog?XR_ACTION_TYPE_FLOAT_INPUT:XR_ACTION_TYPE_BOOLEAN_INPUT).subactionPaths(stack.longs(c.hands));
  if(xrCreateAction((XrActionSet)c.set,info,pointer)!=XR_SUCCESS)return null;return new XrAction(pointer.get(0),(XrActionSet)c.set);
 }
 public long path(ControllerTouch.Context c,String text,MemoryStack stack){var result=stack.mallocLong(1);return xrStringToPath((XrInstance)c.instance,stack.UTF8(text),result)==XR_SUCCESS?result.get(0):0;}
 public int suggest(ControllerTouch.Context c,ControllerProfiles.Profile profile,List<ControllerTouch.Bound> bindings,MemoryStack stack){
  var union=XrActionSuggestedBinding.calloc(bindings.size(),stack);for(int n=0;n<bindings.size();n++){var binding=bindings.get(n);long path=path(c,binding.path(),stack);if(path==0)return XR_ERROR_PATH_INVALID;union.get(n).action((XrAction)binding.action()).binding(path);}
  return xrSuggestInteractionProfileBindings((XrInstance)c.instance,XrInteractionProfileSuggestedBinding.calloc(stack).type$Default().interactionProfile(path(c,profile.id(),stack)).suggestedBindings(union));
 }
 public long profile(ControllerTouch.Context c,int side,MemoryStack stack){var state=XrInteractionProfileState.calloc(stack).type$Default();return xrGetCurrentInteractionProfile((XrSession)c.session,c.hands[side],state)==XR_SUCCESS?state.interactionProfile():0;}
 private XrActionStateGetInfo get(ControllerTouch.Context c,Object action,int side,MemoryStack stack){return XrActionStateGetInfo.calloc(stack).type$Default().action((XrAction)action).subactionPath(c.hands[side]);}
 public ControllerTouch.Contact contact(ControllerTouch.Context c,Object action,int side,MemoryStack stack){var state=XrActionStateBoolean.calloc(stack).type$Default();if(xrGetActionStateBoolean((XrSession)c.session,get(c,action,side,stack),state)!=XR_SUCCESS||!state.isActive())return ControllerTouch.Contact.UNKNOWN;return new ControllerTouch.Contact(true,state.currentState());}
 public ControllerTouch.Value value(ControllerTouch.Context c,Object action,int side,MemoryStack stack){var state=XrActionStateFloat.calloc(stack).type$Default();if(xrGetActionStateFloat((XrSession)c.session,get(c,action,side,stack),state)!=XR_SUCCESS||!state.isActive())return ControllerTouch.Value.UNKNOWN;return new ControllerTouch.Value(true,state.currentState());}
}
