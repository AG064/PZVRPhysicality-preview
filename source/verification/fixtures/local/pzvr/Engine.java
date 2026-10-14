package local.pzvr;
public final class Engine {
 public static volatile XrBackend.Frame latestInput;
 public static volatile XrBackend.Frame latest;
 public static double playspaceYaw;
 public static Math3.Pose origin;
 public static Math3.Pose trackingOrigin(){return origin;}
 public static double scale=1;
 public static Runnable onRigOffset;
 public static Math3.Vec rigOffset(Math3.Vec value){var hook=onRigOffset;onRigOffset=null;if(hook!=null)hook.run();return new Math3.Vec(value.x()*scale,value.y()*scale,value.z()*scale);}
 public static boolean worldPresent,enabled;
 public static boolean active(){return enabled;}
}
