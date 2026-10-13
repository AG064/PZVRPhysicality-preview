package pzvrphysicality;

import java.lang.reflect.Field;import org.joml.Matrix4f;import org.joml.Vector3f;
import viewpoint.render.*;import zombie.characters.IsoPlayer;import zombie.core.skinnedmodel.model.ModelInstance;import static org.lwjgl.opengl.GL33.*;

/** Consumes frame-local native handles without changing the original food, tool, draw packet or XR lifecycle. */
public final class BiteRenderHook {
 private static Field context,targets,iris,outputFbo,viewport;private static BiteRenderer renderer;private static boolean failed;private static volatile long requestedReset;private static long appliedReset;
 private static long loaded=-1,epoch=-1;private static int side=-1;private static ItemMeshCpu.Data source;private static String signature;private static BiteGeometry.Portion portion;
 private static volatile String detail="Utensil portion visuals waiting";
 public static String status(){return detail;}
 public static synchronized void reset(){requestedReset++;}
 private static void clear(){loaded=epoch=-1;side=-1;source=null;signature=null;portion=null;}
 private static Vector3f vector(MouthMath.Vec value){return new Vector3f((float)value.x(),(float)value.y(),(float)value.z());}
 private static void recover(ModelInstance instance){if(instance!=null&&instance.model!=null&&instance.model.mesh!=null&&BiteMeshCpu.get(instance.model.mesh)==null)BiteMeshCpu.recover(instance.model.mesh,System.nanoTime());}
 public static void draw(SceneData scene){
  long reset=requestedReset;if(reset!=appliedReset){clear();failed=false;detail="Utensil portion visuals waiting";appliedReset=reset;}
  if(failed||!Main.ready()||!NativeConsumption.enabled()){clear();return;}
  long now=System.nanoTime();var rig=WorldRigCapture.forScene(scene,now);var actor=IsoPlayer.players.length==0?null:IsoPlayer.players[0];
  if(rig==null||actor==null||actor!=rig.actor()||!rig.fresh(now)){clear();return;}
  try{
   if(context==null){context=field(WorldRenderer.class,"frame");targets=field(WorldRenderer.class,"targets");iris=field(WorldRenderer.class,"irisDrawn");outputFbo=field(FrameContext.class,"outputDrawFbo");viewport=field(FrameContext.class,"viewport");}
   var frame=(FrameContext)context.get(null);var output=(Targets)targets.get(null);if(frame==null||output==null||frame.scene!=scene)return;
   if(iris.getBoolean(null)||frame.panini!=null&&frame.panini.on()){detail="Portion visuals require native perspective output";clear();return;}
   int[] actual=new int[4];glGetIntegerv(GL_VIEWPORT,actual);if(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)!=outputFbo.getInt(frame)||!java.util.Arrays.equals(actual,(int[])viewport.get(frame)))return;
   // Read only a supported held source/tool. This also makes already-uploaded models available to exact contact checks.
   var primary=actor.getPrimaryHandItem();var secondary=actor.getSecondaryHandItem();
   if(UtensilCapture.supportedTool(primary)||UtensilCapture.supportedFood(primary))recover(actor.primaryHandModel);
   if(UtensilCapture.supportedTool(secondary)||UtensilCapture.supportedFood(secondary))recover(actor.secondaryHandModel);
   var bite=NativeConsumption.bite();var visual=UtensilCapture.visual(scene,actor,bite,now);if(visual==null){detail=bite==null?"No loaded utensil portion":"Portion visual waiting for a fresh source/tool";clear();return;}
   if(loaded!=bite.loaded()||epoch!=visual.epoch()||side!=visual.side()){clear();loaded=bite.loaded();epoch=visual.epoch();side=visual.side();}
   var snapshot=BiteMeshCpu.snapshot(visual.foodMesh());if(snapshot==null){detail="Portion visual waiting for original UV geometry";return;}
   if(snapshot.geometry()!=visual.foodGeometry()||ItemMeshCpu.get(visual.foodMesh())!=visual.foodGeometry())return;
   var mesh=snapshot.mesh();var tool=new Matrix4f().set(visual.toolMatrix());
   if(source==null){
    source=snapshot.geometry();signature=snapshot.signature();portion=null;
    try{
     var food=new Matrix4f().set(visual.loadedFoodMatrix());var loadTool=new Matrix4f().set(visual.loadedToolMatrix());
     var centre=food.transformPosition(vector(visual.foodContactLocal()));var edible=new BiteGeometry.Mesh(mesh.vertices(),visual.edibleFaces());
     var candidate=BiteGeometry.crop(edible,food,loadTool,centre,(float)(.012*visual.units()));var centreLocal=new Matrix4f(loadTool).invert().transformPosition(new Vector3f(centre));
     portion=BiteGeometry.recenter(candidate,centreLocal,vector(visual.tipLocal()));
    }catch(IllegalArgumentException unsupported){detail="Portion crop unavailable; native eating retained";return;}
   }
   if(source!=snapshot.geometry()||!signature.equals(snapshot.signature())){portion=null;detail="Portion source changed; take a new bite";return;}
   if(portion==null)return;var model=visual.foodModelInstance();var texture=model.tex!=null?model.tex:model.model==null?null:model.model.tex;
   if(texture==null||!texture.isReady()||texture.isDestroyed()){detail="Portion visual waiting for original food texture";return;}
   int id=texture.getID();if(!glIsTexture(id))return;
   var current=BiteMeshCpu.snapshot(visual.foodMesh());if(NativeConsumption.bite()!=bite||!NativeConsumption.enabled()||UtensilCapture.visual(scene,actor,bite,System.nanoTime())==null||current==null||current.geometry()!=source||!current.signature().equals(signature)){clear();return;}
   float[] tint={colour(model.tintR),colour(model.tintG),colour(model.tintB)};float[] uv={texture.getXStart(),texture.getYStart(),texture.getXEnd(),texture.getYEnd()};
   if(renderer==null)renderer=new BiteRenderer();renderer.draw(portion,tool,frame.viewProjection,id,tint,uv,output.depth,frame.outputWidth,frame.outputHeight);detail="Original edible portion attached to utensil";
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){failed=true;clear();detail="Portion visuals unavailable; native eating retained";System.err.println("[PZVR Physicality] Portion visuals unavailable: "+unavailable);}
 }
 private static float colour(float value){if(!Float.isFinite(value))throw new IllegalArgumentException("Original food tint");return Math.max(0,Math.min(1,value));}
 private static Field field(Class<?> owner,String name)throws ReflectiveOperationException{var f=owner.getDeclaredField(name);f.setAccessible(true);return f;}
 private BiteRenderHook(){}
}
