package pzvrphysicality;
/** Primitive route gates keep disabled optional SDK adapters out of input resolution. */
public final class RouteModes {
 private static volatile boolean body,curtains,crafting,backpack,heldContacts,world;
 public static boolean body(){return body;}public static boolean curtains(){return curtains;}public static boolean crafting(){return crafting;}
 static void body(boolean value){body=value;}static void curtains(boolean value){curtains=value;}static void crafting(boolean value){crafting=value;}
 public static boolean backpack(){return backpack;}public static boolean heldContacts(){return heldContacts;}public static boolean world(){return world;}
 static void backpack(boolean value){backpack=value;}static void heldContacts(boolean value){heldContacts=value;}static void world(boolean value){world=value;}
 private RouteModes(){}
}
