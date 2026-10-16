package pzvrphysicality;
import java.net.*;import java.nio.file.*;import java.util.*;

/** A deny loader detects even swallowed attempts to resolve native optional classes through an Off path. */
public final class PackCurtainIsolationTest {
 public static final class Probe {
  public static int run(){int n=0;if(PackCurtainCapture.enabled())throw new AssertionError("Default Off");n++;if(!PackCurtainCapture.contacts(null).isEmpty())throw new AssertionError("Off contacts");n++;if(PackCurtainCapture.owns(null))throw new AssertionError("Null ownership");n++;if(PackCurtainCapture.owns(new zombie.iso.IsoObject()))throw new AssertionError("Generic furniture ownership");n++;PackCurtainCapture.configure(false);if(!PackCurtainCapture.status().contains("Off"))throw new AssertionError("Off status");return n+1;}
 }
 public static void main(String[] args)throws Exception{var denied=new ArrayList<String>();var urls=new URL[args.length];for(int n=0;n<args.length;n++)urls[n]=Path.of(args[n]).toUri().toURL();int count;
  try(var loader=new URLClassLoader(urls,ClassLoader.getPlatformClassLoader()){
   protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{if((name.startsWith("zombie.")||name.startsWith("viewpoint.")||name.startsWith("local.pzvr.")||name.startsWith("techjungle."))&&!name.equals("zombie.iso.IsoObject")&&!name.equals("zombie.characters.IsoPlayer")){denied.add(name);throw new ClassNotFoundException("Optional native class denied: "+name);}return super.loadClass(name,resolve);}
  }){count=(Integer)loader.loadClass("pzvrphysicality.PackCurtainIsolationTest$Probe").getMethod("run").invoke(null);}
  if(!denied.isEmpty())throw new AssertionError("Off path attempted native resolution: "+denied);System.out.println("Pack curtain isolation: "+(count+1)+" checks passed");
 }
 private PackCurtainIsolationTest(){}
}
