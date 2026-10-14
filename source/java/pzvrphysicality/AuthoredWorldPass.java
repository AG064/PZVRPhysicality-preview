package pzvrphysicality;
import viewpoint.render.SceneData;import static org.lwjgl.opengl.GL33.*;
/** All authored finish advice reaches one ordered pass, preserving each hook's native eligibility gates. */
public final class AuthoredWorldPass {
 public static void draw(SceneData scene){if(!AuthoredDepth.enabled())return;
  try{int[] viewport=new int[4];glGetIntegerv(GL_VIEWPORT,viewport);var decision=AuthoredDepth.startPassDecision(scene,glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING),viewport);if(decision==AuthoredDepth.PassDecision.INVALID){reject();return;}if(decision!=AuthoredDepth.PassDecision.ACCEPT)return;
   try{WorldVisualRenderHook.draw(scene);MoveableRenderHook.draw(scene);WindowMotionRenderHook.drawOpaque(scene);BiteRenderHook.draw(scene);TapRenderHook.draw(scene);WindowMotionRenderHook.drawGlass(scene);}finally{AuthoredDepth.endPass(scene);}
  }catch(RuntimeException|LinkageError unavailable){reject();}
 }
 private static void reject(){WorldVisuals.rejectRenderReceipt();MoveableRenderHook.rejectScope();WindowMotionRenderHook.rejectScope();}
 private AuthoredWorldPass(){}
}
