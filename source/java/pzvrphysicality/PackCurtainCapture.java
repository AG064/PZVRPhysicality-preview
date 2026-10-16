package pzvrphysicality;

import java.util.List;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoObject;

/** Optional facade. Generic furniture and Off paths never resolve the typed curtain/provider implementation. */
public final class PackCurtainCapture {
 private static volatile boolean configured;
 public static boolean enabled(){return configured;}
 public static String status(){return configured?PackCurtainState.status():"Pack curtains Off";}
 public static synchronized void configure(boolean value){
  if(configured==value)return;
  if(!value){configured=false;PackCurtainState.configure(false);return;}
  try{PackCurtainState.configure(true);configured=true;}
  catch(RuntimeException|LinkageError failure){configured=false;try{PackCurtainState.configure(false);}catch(RuntimeException|LinkageError cleanup){failure.addSuppressed(cleanup);}throw failure;}
 }
 public static boolean owns(IsoObject owner){return owner!=null&&owner.getClass().getName().equals("zombie.iso.objects.IsoCurtain")&&PackCurtainState.owns(owner);}
 public static List<PhysicalWorld.Static> contacts(IsoPlayer actor){return configured?PackCurtainState.contacts(actor):List.of();}
 static boolean alias(String name){return PackCurtainState.alias(name);}
 static void discovery(IsoPlayer actor){if(configured)PackCurtainState.discovery(actor);}
 static void request(IsoObject owner){if(configured)PackCurtainState.requestObject(owner);}
 static PackCurtainState.Packet packet(IsoObject owner){return configured?PackCurtainState.packetObject(owner):null;}
 static boolean current(PackCurtainState.Packet packet){return configured&&PackCurtainState.current(packet);}
 private PackCurtainCapture(){}
}
