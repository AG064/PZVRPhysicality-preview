package pzvrphysicality;
public final class BiteRenderHook {
 public static int calls;public static viewpoint.render.SceneData scene;
 public static void draw(viewpoint.render.SceneData value){calls++;scene=value;}
}
