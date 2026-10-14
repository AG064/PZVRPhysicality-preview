package pzvrphysicality;
import java.lang.reflect.Field;import viewpoint.render.*;import static org.lwjgl.opengl.GL33.*;
/** Uses each native eye finish scope; owns no OpenXR session, camera or frame target. */
public final class WorldVisualRenderHook {
 private static FaucetRenderer renderer;private static Field context,targets,iris,outputFbo,viewport;private static volatile boolean failed;
 public static boolean failed(){return failed;}
 public static void draw(SceneData scene){long now=System.nanoTime();if(failed||!WorldVisuals.enabled()||!Main.ready())return;var data=WorldVisuals.forScene(scene,now);if(data.rows().isEmpty()||!data.fresh(now))return;var rig=WorldRigCapture.forScene(scene,now);if(rig==null||rig.generation()!=data.generation()||!WorldVisuals.eligible(zombie.characters.IsoPlayer.players.length==0?null:zombie.characters.IsoPlayer.players[0],rig,now))return;
  try{if(context==null){context=field(WorldRenderer.class,"frame");targets=field(WorldRenderer.class,"targets");iris=field(WorldRenderer.class,"irisDrawn");outputFbo=field(FrameContext.class,"outputDrawFbo");viewport=field(FrameContext.class,"viewport");}var frame=(FrameContext)context.get(null);var output=(Targets)targets.get(null);if(frame.scene!=scene)return;if(iris.getBoolean(null)||frame.panini!=null&&frame.panini.on())throw new IllegalStateException("World additions require native perspective depth");int[] actual=new int[4];glGetIntegerv(GL_VIEWPORT,actual);if(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)!=outputFbo.getInt(frame)||!java.util.Arrays.equals(actual,(int[])viewport.get(frame)))throw new IllegalStateException("World output scope changed");if(renderer==null)renderer=new FaucetRenderer();renderer.draw(data,rig.camera(),frame.viewProjection,output.depth,frame.outputWidth,frame.outputHeight);WorldVisuals.drawn(data);}
  catch(ReflectiveOperationException|RuntimeException|LinkageError error){failed=true;if(renderer!=null){try{renderer.close();}catch(RuntimeException|LinkageError ignored){}renderer=null;}System.err.println("[PZVR Physicality] World additions unavailable: "+error);}
 }
 private static Field field(Class<?> owner,String name)throws ReflectiveOperationException{var f=owner.getDeclaredField(name);f.setAccessible(true);return f;}
 private WorldVisualRenderHook(){}
}
