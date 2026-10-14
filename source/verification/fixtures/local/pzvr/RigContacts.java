package local.pzvr;
public final class RigContacts {
 public record HandShape(int hand,Object item,ContactMath.Capsule volume,Math3.Vec playerPosition,Math3.Pose grip,Math3.Vec head,double yaw){}
 public record Snapshot(long hostTime,long inputTime,java.util.List<HandShape> hands){}
 public static volatile Snapshot latest;
 public static int captured;
 public static void capture(Object frame,Object actor,XrBackend.Frame input){captured++;}
}
