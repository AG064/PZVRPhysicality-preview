package pzvrphysicality;
public final class GripRouteTest {
 public static void main(String[] args){int n=0;for(boolean active:new boolean[]{false,true})for(String key:new String[]{"dominant.grip","other.grip","ui.grip","dominant.trigger","other.secondary","other.menu"})for(String binding:new String[]{"grab","custom","none"}){
  String result=GripRoute.resolve(active,key,binding);boolean owned=active&&(key.equals("dominant.grip")||key.equals("other.grip"));if(!result.equals(owned?"none":binding))throw new AssertionError("Unrelated upstream input changed");n++;
  }
  for(int dominant:new int[]{-1,0,1,2})for(String key:new String[]{"dominant.grip","other.grip","ui.grip","dominant.trigger",null}){
   int expected=dominant>=0&&dominant<=1&&GripRoute.gripKey(key)?("dominant.grip".equals(key)?dominant:1-dominant):-1;
   if(GripRoute.side(key,dominant)!=expected)throw new AssertionError("Grip key must map to only its matching native hand");n++;
  }
  if(!"custom".equals(GripRoute.resolve(true,null,"custom")))throw new AssertionError("Unknown key must retain its native binding");n++;
  System.out.println("Physical grip ownership: "+n+" scoped, per-hand and saved-binding-preservation checks passed");}
}
