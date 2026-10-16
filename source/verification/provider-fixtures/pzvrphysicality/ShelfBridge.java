package pzvrphysicality;
public final class ShelfBridge {
 public record Binding(viewpoint.packs.PackBind binding,boolean turns,boolean empty,long generation,long time){}
 public static boolean supported,ready;public static int requests,clears;public static Binding binding;
 public static boolean supported(zombie.iso.IsoObject owner){return supported;}
 public static boolean ready(zombie.iso.IsoObject owner){return ready;}
 public static Binding binding(zombie.iso.IsoObject owner){return binding;}
 public static void request(zombie.iso.IsoObject owner,boolean enabled){requests++;}
 public static void clear(){clears++;}
}
