package pzvrphysicality;
 /** Maps only the matching tracked grip gesture. Saved upstream bindings are never modified. */
public final class GripRoute {
 public static boolean gripKey(String key){return "dominant.grip".equals(key)||"other.grip".equals(key);}
 public static int side(String key,int dominant){if(!gripKey(key)||dominant<0||dominant>1)return -1;return "dominant.grip".equals(key)?dominant:1-dominant;}
 public static String resolve(boolean active,String key,String original){return active&&gripKey(key)?"none":original;}
 private GripRoute(){}
}
