package local.pzvr;
public final class Bindings {public static int calls;public static String resolve(Object settings,String key){calls++;return "native:"+key;}}
