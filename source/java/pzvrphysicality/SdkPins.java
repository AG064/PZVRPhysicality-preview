package pzvrphysicality;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Reject unknown SDK owners before the extension takes any gameplay input. */
public final class SdkPins {
 public static final String GAME="e1a69eb743ede60b213a0fe7f8b83d4fcab773036d256cc4543a336f3b058a33";
 public static final String PZVR="c44c165ca485d24211d1455e792a90b9ca2a44ee478c71a61240a7af4c348e3e";
 public static final String VIEWPOINT="afa5829d16b3785b0e62673376da6d2fb4c16b0ba3966d9d4a4ea064f584edc8";
 public static final String BUDDY="eb79b9876332010733a8e0d7ce4fe0377846059a9e28859029e2a0fb449f6cf2";
 public static final String SHELVES="cb111d813c3436b0355c44a65f9d38fb5ebb489a4497f10caaa2027240a116cf";
 public static void verify(Class<?> type,String expected)throws Exception {
  var source=type.getProtectionDomain().getCodeSource();
  if(source==null||!source.getLocation().getProtocol().equals("file"))throw new IllegalStateException("Unknown SDK owner: "+type.getName());
  var file=Path.of(source.getLocation().toURI());
  if(!Files.isRegularFile(file)||!file.getFileName().toString().endsWith(".jar"))throw new IllegalStateException("SDK is not a JAR: "+type.getName());
  var digest=MessageDigest.getInstance("SHA-256");
  try(var input=Files.newInputStream(file)){var buffer=new byte[65536];int length;while((length=input.read(buffer))!=-1)digest.update(buffer,0,length);}
  if(!HexFormat.of().formatHex(digest.digest()).equals(expected))throw new IllegalStateException("Unsupported SDK: "+type.getName());
 }
 public static void verifyRuntime()throws Exception {
  verify(zombie.characters.IsoPlayer.class,GAME);verify(local.pzvr.Engine.class,PZVR);
  verify(viewpoint.render.PackModel.class,VIEWPOINT);verify(me.zed_0xff.zombie_buddy.Patch.class,BUDDY);
  verify(Class.forName("pz.viewpointcontainers.ContainerStates",false,SdkPins.class.getClassLoader()),SHELVES);
 }
 private SdkPins(){}
}
