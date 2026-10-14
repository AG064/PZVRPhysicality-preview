package pzvrphysicality;
import local.pzvr.Engine;import local.pzvr.VrApi;
/** Explicit native HUD requests. No synthetic input or private HUD field writes. */
public final class GuideUiRoute {
 private static volatile String result="Guide has not requested the native HUD";
 private GuideUiRoute(){}
 public static boolean show(boolean ready){if(!ready){result="SDK validation pending";return false;}try{VrApi.showHud();boolean open=VrApi.fullHud();result=open?"Native HUD open for the guide":"Native HUD request unavailable";return open;}catch(RuntimeException|LinkageError error){result="Native HUD request unavailable: "+error.getClass().getSimpleName();return false;}}
 public static boolean returnToGame(boolean ready){if(!ready){result="SDK validation pending";return false;}try{if(VrApi.fullHud())Engine.toggleHud();boolean closed=!VrApi.fullHud();result=closed?"Native HUD closed; return to physical interaction":"Native menu dismissed or HUD still open; press Return to game again";return closed;}catch(RuntimeException|LinkageError error){result="Native HUD close unavailable: "+error.getClass().getSimpleName();return false;}}
 public static String status(){return result;}
 public static String status(boolean ready){if(!ready)return "Current native HUD unavailable: SDK validation pending; last request="+result;try{return "Current native HUD="+VrApi.fullHud()+"; last request="+result;}catch(RuntimeException|LinkageError unavailable){return "Current native HUD unavailable; last request="+result;}}
 public static String controls(boolean ready){if(!ready)return "HUD controls unavailable until SDK validation";try{int count=VrApi.bindingCount();if(count<0||count>64)return "Native HUD binding count unavailable";StringBuilder text=new StringBuilder();for(int i=0;i<count;i++){String slot=VrApi.bindingSlot(i),value=VrApi.bindingValue(slot);if("hud".equals(value)||"hudRadial".equals(value)){if(text.length()>0)text.append("; ");text.append(slot).append("=").append(value);}}return text.length()==0?"No controller HUD action bound; use native PZVR Controller bindings":text+". hudRadial: tap HUD, hold radial. Native PZVR Controller bindings remain authoritative.";}catch(RuntimeException|LinkageError error){return "Native HUD bindings unavailable";}}
}
