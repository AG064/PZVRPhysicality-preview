package pzvrphysicality;
import java.nio.file.*;
import java.util.*;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import static net.bytebuddy.matcher.ElementMatchers.*;
/** Current Light API initializer contracts, inspected without Class.forName or native execution. */
final class LightReflectionContract {
 static int lastChecks;
 static TypePool pool;
 static TypeDescription type(String name){return pool.describe(name).resolve();}
 static TypeDescription field(String owner,String name,boolean publicField){
  var fields=type(owner).getDeclaredFields().filter(named(name));
  if(fields.size()!=1||publicField&&!fields.getOnly().isPublic())throw new AssertionError("Current Light reflected field "+owner+"."+name);
  lastChecks++;return fields.getOnly().getType().asErasure();
 }
 static TypeDescription method(String owner,String name,boolean publicMethod,boolean staticMethod,String...arguments){
  var current=type(owner);var found=new ArrayList<net.bytebuddy.description.method.MethodDescription.InDefinedShape>();
  while(current!=null){for(var candidate:current.getDeclaredMethods().filter(named(name)))if(candidate.getParameters().asTypeList().asErasures().stream().map(TypeDescription::getName).toList().equals(List.of(arguments))&&(!publicMethod||candidate.isPublic()))found.add(candidate);if(!found.isEmpty()||!publicMethod)break;current=current.getSuperClass()==null?null:current.getSuperClass().asErasure();}
  if(found.size()!=1||publicMethod&&!found.getFirst().isPublic()||found.getFirst().isStatic()!=staticMethod)throw new AssertionError("Current Light reflected method "+owner+"."+name+List.of(arguments));
  lastChecks++;return found.getFirst().getReturnType().asErasure();
 }
 public static void main(String[] args)throws Exception {
  try(var locator=new ClassFileLocator.Compound(ClassFileLocator.ForJarFile.of(Path.of(args[0]).toFile()),ClassFileLocator.ForJarFile.of(Path.of(args[1]).toFile()),ClassFileLocator.ForClassLoader.of(LightReflectionContract.class.getClassLoader()))){
   pool=TypePool.Default.of(locator);
   for(String name:new String[]{"scene","camX","camY","camZ"})field("viewpoint.core.Frame",name,true);
   for(String name:new String[]{"spots","torchCount","torches"})field("viewpoint.render.SceneData",name,true);
   String relief=field("viewpoint.render.SceneData","relief",true).getName();
   for(String name:new String[]{"instances","keys"})field("viewpoint.render.ModelDraws",name,false);
   for(String name:new String[]{"object","parent"})field("zombie.core.skinnedmodel.model.ModelInstance",name,false);
   field("viewpoint.light.Headlights","origin",false);
   method("viewpoint.light.Headlights","slotOf",false,true,"zombie.vehicles.BaseVehicle");
   method("viewpoint.light.Lights","lift",false,true,"viewpoint.render.SceneData","float","float");
   method("viewpoint.models.VehicleTilt","tilt",false,true,"viewpoint.core.Frame",relief,"zombie.vehicles.BaseVehicle","float","float");
   String vehicle="zombie.vehicles.BaseVehicle",part="zombie.vehicles.VehiclePart";
   method(part,"getVehicle",true,false);method(part,"getId",true,false);method(part,"getTable",true,false,"java.lang.String");
   String light=method(part,"getLight",true,false).getName();for(String name:new String[]{"offset","dot"})field(light,name,true);
   String script=method(vehicle,"getScript",true,false).getName();method(script,"getExtents",true,false);method(script,"getFullName",true,false);
   method(vehicle,"getWorldPos",true,false,"float","float","float","org.joml.Vector3f");
   String square=method(vehicle,"getCurrentSquare",true,false).getName();method(square,"getZ",true,false);
   String table=method(vehicle,"getModData",true,false).getName();method(table,"rawget",true,false,"java.lang.Object");
   for(String name:new String[]{"x","y","z"})field("org.joml.Vector3f",name,true);
   if(type("org.joml.Vector3f").getDeclaredMethods().filter(isConstructor().and(takesArguments(0)).and(isPublic())).size()!=1)throw new AssertionError("Current Light vector constructor");lastChecks++;
  }
  System.out.println("Light reflected API: "+lastChecks+" metadata contracts passed; no original definitions loaded");
 }
}
