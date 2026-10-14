package pzvrphysicality;
import org.joml.Quaternionf;

/** World-space collision rotation relative to the free-space motor reference, with detached numeric inputs. */
public final class HeldContactRotation {
 public static Quaternionf difference(HeldContactWorld.Response response){if(response==null)return null;var actual=response.pose();var reference=response.reference();if(actual.length!=7||reference.length!=7)return null;double a=0,b=0;for(int n=3;n<7;n++){if(!Float.isFinite(actual[n])||!Float.isFinite(reference[n]))return null;a+=actual[n]*actual[n];b+=reference[n]*reference[n];}if(Math.abs(a-1)>.002||Math.abs(b-1)>.002)return null;var delta=new Quaternionf(actual[3],actual[4],actual[5],actual[6]).normalize().mul(new Quaternionf(reference[3],reference[4],reference[5],reference[6]).normalize().conjugate()).normalize();if(delta.w<0)delta.set(-delta.x,-delta.y,-delta.z,-delta.w);return delta;}
 public static double angle(HeldContactWorld.Response response){var delta=difference(response);return delta==null?0:2*Math.acos(Math.min(1,Math.max(-1,delta.w)));}
 private HeldContactRotation(){}
}
