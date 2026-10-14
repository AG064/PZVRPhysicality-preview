package pzvrphysicality;
public final class TapRenderHook {
 public static int calls;public static Object scene;
 public static void draw(viewpoint.render.SceneData value){calls++;scene=value;}
 public static boolean failed(){return false;}
}
