package pzvrphysicality;

import java.lang.reflect.Field;import viewpoint.render.*;import static org.lwjgl.opengl.GL33.*;

/** Draw in the native finish scope for each real eye; never opens an XR session or owns frame targets. */
public final class TapRenderHook {
 private static TapRenderer renderer;private static Field context,targets,iris,outputFbo,viewport;private static volatile boolean failed;
 public static boolean failed(){return failed;}
 public static void draw(SceneData scene){
  var data=NativeTaps.visuals;long now=System.nanoTime();if(failed||!Main.ready()||data.dots().isEmpty()&&data.streams().isEmpty()||!data.fresh(now))return;
  var rig=WorldRigCapture.forScene(scene,now);if(rig==null||rig.generation()!=data.generation())return;
  try{
   if(context==null){context=field(WorldRenderer.class,"frame");targets=field(WorldRenderer.class,"targets");iris=field(WorldRenderer.class,"irisDrawn");outputFbo=field(FrameContext.class,"outputDrawFbo");viewport=field(FrameContext.class,"viewport");}
   var frame=(FrameContext)context.get(null);var output=(Targets)targets.get(null);if(frame.scene!=scene)return;
   // Only the audited native near-depth/perspective output is supported by this pilot.
   // Iris and Panini need their own depth and projection adapters before interaction is safe.
   if(iris.getBoolean(null)||frame.panini!=null&&frame.panini.on())throw new IllegalStateException("Tap pilot requires native perspective rendering");
   int[] actual=new int[4];glGetIntegerv(GL_VIEWPORT,actual);
   if(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)!=outputFbo.getInt(frame)||!java.util.Arrays.equals(actual,(int[])viewport.get(frame)))throw new IllegalStateException("Native tap output scope changed");
   if(renderer==null)renderer=new TapRenderer();renderer.draw(data,rig.camera(),frame.viewProjection,output.depth,frame.outputWidth,frame.outputHeight);
  }catch(ReflectiveOperationException|RuntimeException|LinkageError error){failed=true;System.err.println("[PZVR Physicality] Tap visuals unavailable: "+error);}
 }
 private static Field field(Class<?> owner,String name)throws ReflectiveOperationException{var f=owner.getDeclaredField(name);f.setAccessible(true);return f;}
 private TapRenderHook(){}
}
