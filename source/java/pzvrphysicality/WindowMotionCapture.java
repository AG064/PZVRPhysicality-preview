package pzvrphysicality;

import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import viewpoint.world.*;
import zombie.core.textures.Texture;
import zombie.iso.IsoDirections;
import zombie.iso.objects.IsoWindow;
import zombie.iso.sprite.IsoSprite;

/** Live-only normal provider pair preparation. No native world state or shared mesh array is changed. */
public final class WindowMotionCapture {
 public record Surface(Object identity,NativeWindowGeometry.Data data,WindowMotionProjection projection){}
 public record Art(Texture texture,Object page,float[] mapping,String mask){
  public Art{mapping=mapping.clone();if(texture==null||page==null||mapping.length!=11||mask==null)throw new IllegalArgumentException("Window original art");for(float value:mapping)if(!Float.isFinite(value))throw new IllegalArgumentException("Window atlas mapping");if(mapping[0]>=mapping[2]||mapping[1]>=mapping[3]||mapping[6]<=0||mapping[7]<=0)throw new IllegalArgumentException("Window atlas extent");}
  public float[] mapping(){return mapping.clone();}
 }
 public record Pair(WindowMotionProfile profile,IsoSprite closedSprite,IsoSprite openSprite,Surface closed,Surface open,Art closedArt,Art openArt,WindowMotionGeometry.Pieces pieces){}
 private static final String[] MASKS={"f87b8f5c175eba9b4140037800ff2fdc8f22557a519233e1982477ca3aa90e63","248dc5c08512466acaf6298c4d014a9dc0a7412cd7b2eff68933cd32fca09d99","703c2e7b7386dd6dd0d52d4f00ca7786e8b4897401671b8d94771e323c4fc172","a7dea6bea8259873fa263d35515ee92d71ea982192ad100cf791acf09245ad31"};
 private Class<?> owner,surfaceType;private Field surfaces,north,plane,mesh,aperture,bounds,closedSprite,projection,triangles;private Method replace,enabled,tile;private boolean unavailable;
 private void verify()throws Exception{
  if(owner!=null)return;if(unavailable)throw new IllegalStateException("Window motion provider unavailable");
  try{for(var game:new Class<?>[]{IsoWindow.class,IsoSprite.class,Texture.class})SdkPins.verify(game,SdkPins.GAME);for(var world:new Class<?>[]{TileMeshes.class,WorldMesher.class})SdkPins.verify(world,SdkPins.VIEWPOINT);var loader=getClass().getClassLoader();var assets=Class.forName("techjungle.betterwindow.WindowAssets",false,loader);var type=Class.forName("techjungle.betterwindow.Surface",false,loader);var main=Class.forName("techjungle.betterwindow.Main",false,loader);for(var c:new Class<?>[]{assets,type,main})SdkPins.verify(c,NativeWindowContacts.PROVIDER_SHA256);
   surfaces=field(assets,"_surfaces");north=type.getField("north");plane=type.getField("plane");mesh=type.getField("mesh");aperture=type.getField("aperture");bounds=type.getField("bounds");projection=field(type,"_projection");triangles=field(Class.forName("techjungle.betterwindow.Projection",false,loader),"_triangles");closedSprite=field(IsoWindow.class,"closedSprite");replace=assets.getMethod("replace",IsoSprite.class,Texture.class,TileMesh.class);enabled=main.getMethod("enabled");tile=TileMeshes.class.getDeclaredMethod("get",IsoSprite.class,Texture.class,IsoSprite.class);tile.setAccessible(true);surfaceType=type;owner=assets;
  }catch(Exception|LinkageError failure){unavailable=true;throw failure;}
 }
 private static Field field(Class<?> type,String name)throws ReflectiveOperationException{var value=type.getDeclaredField(name);value.setAccessible(true);return value;}
 private Surface cached(IsoSprite sprite)throws ReflectiveOperationException{
  synchronized(owner){var raw=surfaces.get(null);if(!(raw instanceof Map<?,?> map))return null;var object=map.get(sprite);if(object==null||object.getClass()!=surfaceType)return null;return new Surface(object,new NativeWindowGeometry.Data(north.getBoolean(object),plane.getFloat(object),(float[])mesh.get(object),(float[])aperture.get(object),(float[])bounds.get(object)),new WindowMotionProjection((float[][])triangles.get(projection.get(object))));}
 }
 private static Art art(IsoSprite sprite)throws Exception{
  var profile=WindowMotionProfile.forSprite(sprite==null?null:sprite.name);if(profile==null)return null;int number=Integer.parseInt(sprite.name.substring("fixtures_windows_01_".length()));var texture=sprite.getTextureForCurrentFrame(IsoDirections.N);if(texture==null||!texture.isReady()||texture.getTextureId()==null||texture.getMask()==null)return null;
  int offsetX=number%2==0?14:73,offsetY=number==3?53:54,height=number==3?94:93;
  if(texture.getWidth()!=39||texture.getHeight()!=height||texture.getWidthOrig()!=128||texture.getHeightOrig()!=256||texture.getOffsetX()!=offsetX||texture.getOffsetY()!=offsetY||WorldMesher.pixelScale(texture)!=2)return null;
  var mask=texture.getMask();if(mask.getWidth()!=39||mask.getHeight()!=height)return null;var digest=MessageDigest.getInstance("SHA-256");for(int y=0;y<height;y++)for(int x=0;x<39;x++)digest.update((byte)(mask.get(x,y)?1:0));String hash=HexFormat.of().formatHex(digest.digest());if(!hash.equals(MASKS[number]))return null;
  return new Art(texture,texture.getTextureId(),WorldMesher.textureMapping(texture),hash);
 }
 public Pair prepare(IsoWindow window,TileMesh winner)throws Exception{
  if(window==null||winner==null||window.getClass()!=IsoWindow.class||window.getSprite()==null||window.isSmashed()||window.isGlassRemoved()||window.isDestroyed())return null;verify();if(!Boolean.TRUE.equals(enabled.invoke(null)))return null;
  var profile=WindowMotionProfile.forSprite(window.getSprite().name);if(profile==null||profile.north()!=window.getNorth())return null;var closed=(IsoSprite)closedSprite.get(window);var open=window.getOpenSprite();if(closed==null||open==null||!profile.closed().equals(closed.name)||!profile.open().equals(open.name)||window.getSprite()!=(window.IsOpen()?open:closed))return null;
  var selected=cached(window.getSprite());if(selected==null||!Arrays.equals(selected.data.mesh(),winner.data))return null;var closedArt=art(closed);var openArt=art(open);if(closedArt==null||openArt==null)return null;
  // These are normal original live cache preparations using existing sprite/texture identities.
  var closedTile=(TileMesh)tile.invoke(null,closed,closedArt.texture,null);var openTile=(TileMesh)tile.invoke(null,open,openArt.texture,null);if(closedTile==null||openTile==null)return null;
  replace.invoke(null,closed,closedArt.texture,closedTile);replace.invoke(null,open,openArt.texture,openTile);var a=cached(closed);var b=cached(open);if(a==null||b==null||a.data.north()!=profile.north()||b.data.north()!=profile.north()||Math.abs(a.data.plane()-b.data.plane())>.0001f)return null;
  return new Pair(profile,closed,open,a,b,closedArt,openArt,WindowMotionGeometry.split(profile,a.data));
 }
 public boolean current(Pair pair,IsoWindow window){
  if(pair==null||window==null||window.getSquare()==null||window.getObjectIndex()<0||window.getSquare().getObjects().indexOf(window)<0||window.isSmashed()||window.isGlassRemoved()||window.isDestroyed()||window.getNorth()!=pair.profile.north()||window.getSprite()!=(window.IsOpen()?pair.openSprite:pair.closedSprite))return false;
  try{if(!Boolean.TRUE.equals(enabled.invoke(null))||closedSprite.get(window)!=pair.closedSprite||window.getOpenSprite()!=pair.openSprite)return false;var a=cached(pair.closedSprite);var b=cached(pair.openSprite);return same(a,pair.closed)&&same(b,pair.open)&&same(art(pair.closedSprite),pair.closedArt)&&same(art(pair.openSprite),pair.openArt);}catch(Exception|LinkageError unavailable){return false;}
 }
 private static boolean same(Surface a,Surface b){return a!=null&&a.identity==b.identity&&b.data.matches(a.data.north(),a.data.plane(),a.data.mesh(),a.data.aperture(),a.data.bounds())&&b.projection.matches(a.projection);}
 private static boolean same(Art a,Art b){return a!=null&&a.texture==b.texture&&a.page==b.page&&a.mask.equals(b.mask)&&Arrays.equals(a.mapping,b.mapping);}
 public WindowMotionCapture(){}
}
