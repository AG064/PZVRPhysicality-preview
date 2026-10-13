package pzvrphysicality;

import java.lang.reflect.*;import java.util.*;
import zombie.characters.IsoPlayer;import zombie.iso.*;import zombie.iso.objects.*;import zombie.iso.sprite.IsoSprite;
import viewpoint.world.WorldMesher;import viewpointvr.physics.CollisionShape;

/** Copies only the selected provider's already-built cloth. No model generation or world mutation. */
public final class CurtainCapture {
 public static final String PROVIDER_SHA256="42f9aecafbb34a4bc76c6a8130720608fed79de9f78f179ef1fca9d4d635d6b1";
 record Reading(Object identity,boolean north,float plane,float[] mesh,float[] aperture,float[] bounds){}
 interface Provider {void verify()throws Exception;boolean enabled()throws ReflectiveOperationException;Reading read(IsoSprite sprite)throws ReflectiveOperationException;}
 private static final class NativeProvider implements Provider {
  private Class<?> owner,type;private Field surfaces,north,plane,mesh,aperture,bounds;private Method enabled;
  public void verify()throws Exception{if(owner!=null)return;var loader=CurtainCapture.class.getClassLoader();Class<?> o=Class.forName("techjungle.betterwindow.WindowAssets",false,loader),s=Class.forName("techjungle.betterwindow.Surface",false,loader),m=Class.forName("techjungle.betterwindow.Main",false,loader);SdkPins.verify(o,PROVIDER_SHA256);SdkPins.verify(s,PROVIDER_SHA256);SdkPins.verify(m,PROVIDER_SHA256);surfaces=o.getDeclaredField("_surfaces");surfaces.setAccessible(true);north=s.getField("north");plane=s.getField("plane");mesh=s.getField("mesh");aperture=s.getField("aperture");bounds=s.getField("bounds");enabled=m.getMethod("enabled");type=s;owner=o;}
  public boolean enabled()throws ReflectiveOperationException{return owner!=null&&Boolean.TRUE.equals(enabled.invoke(null));}
  public Reading read(IsoSprite sprite)throws ReflectiveOperationException{if(owner==null||sprite==null||!enabled())return null;synchronized(owner){Object raw=surfaces.get(null);if(!(raw instanceof Map<?,?> values))return null;Object value=values.get(sprite);if(value==null||value.getClass()!=type)return null;return new Reading(value,north.getBoolean(value),plane.getFloat(value),(float[])mesh.get(value),(float[])aperture.get(value),(float[])bounds.get(value));}}
 }
 static final class Data {
  final java.lang.ref.WeakReference<Object> identity;final boolean north;final float plane;final float[] mesh,aperture,bounds;final CurtainPolicy.Panel panel;final int bytes;
  Data(Reading r){if(r.identity==null||r.mesh.length>NativeWindowGeometry.MAX_FLOATS||r.mesh.length%24!=0||r.aperture.length>NativeWindowGeometry.MAX_APERTURE_FLOATS||r.aperture.length%24!=0||r.bounds.length!=6||!Float.isFinite(r.plane)||Math.abs(r.plane)>4)throw new IllegalArgumentException("Curtain provider budget");identity=new java.lang.ref.WeakReference<>(r.identity);north=r.north;plane=r.plane;mesh=r.mesh.clone();aperture=r.aperture.clone();bounds=r.bounds.clone();for(float value:aperture)if(!Float.isFinite(value))throw new IllegalArgumentException("Curtain aperture");for(int at=0;at<mesh.length;at+=8)for(int axis=0;axis<3;axis++)if(mesh[at+axis]<bounds[axis]-.03f||mesh[at+axis]>bounds[axis+3]+.03f)throw new IllegalArgumentException("Curtain mesh/envelope mismatch");var shape=NativeWindowGeometry.read(mesh);if(shape==null)throw new IllegalArgumentException("No opaque curtain cloth");var vertices=shape.vertices();var faces=shape.faces();panel=new CurtainPolicy.Panel(vertices,faces,north,new double[]{bounds[0],bounds[2],bounds[1],bounds[3],bounds[5],bounds[4]});bytes=(mesh.length+aperture.length+bounds.length+vertices.length+faces.length)*4;}
  boolean matches(Reading r){return r!=null&&r.identity==identity.get()&&r.north==north&&Float.floatToIntBits(r.plane)==Float.floatToIntBits(plane)&&Arrays.equals(mesh,r.mesh)&&Arrays.equals(aperture,r.aperture)&&Arrays.equals(bounds,r.bounds);}
 }
 public record Target(IsoCurtain source,IsoGridSquare square,IsoSprite sprite,IsoObject attached,boolean open,ContactFrame.Point base,Data data){
  CurtainPolicy.Point local(ContactFrame.Point p){return new CurtainPolicy.Point(p.x()-base.x(),p.y()-base.y(),p.height()-base.height());}
  ContactFrame.Point world(CurtainPolicy.Point p){return new ContactFrame.Point(base.x()+p.x(),base.y()+p.y(),base.height()+p.z());}
 }
 private final WeakHashMap<Object,Data> cache=new WeakHashMap<>();private Provider provider=new NativeProvider();private String status="Curtain geometry waiting";
 public void verify()throws Exception{provider.verify();}
 public String status(){return status;}
 public void clear(){cache.clear();status="Curtain geometry waiting";}
 void providerForTest(Provider value){provider=value;clear();}
 private Data data(IsoSprite sprite)throws ReflectiveOperationException{var r=provider.read(sprite);if(r==null||r.mesh==null||r.aperture==null||r.bounds==null)return null;var previous=cache.get(r.identity);if(previous!=null&&previous.matches(r))return previous;var copied=new Data(r);int total=cache.values().stream().mapToInt(value->value.bytes).sum()-(previous==null?0:previous.bytes);if(cache.size()>=32&&!cache.containsKey(r.identity)||total+copied.bytes>4*1024*1024)cache.clear();cache.put(r.identity,copied);return copied;}
 private static ContactFrame.Point base(IsoObject source){var square=source.getSquare();return new ContactFrame.Point(square.getX()+.5,square.getY()+.5,square.getZ()*Math.sqrt(6)+WorldMesher.rise(source));}
 public List<Target> nearby(IsoPlayer player){
  if(player==null||player.getSquare()==null||player.getCell()==null)return List.of();var result=new ArrayList<Target>();int scanned=0;var here=player.getSquare();
  try{if(!provider.enabled()){status="Selected BetterWindow provider unavailable";return List.of();}
   outer:for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){var square=player.getCell().getGridSquare(here.getX()+dx,here.getY()+dy,here.getZ());if(square==null||!square.isAdjacentTo(here))continue;var objects=square.getObjects();if(objects==null)continue;
    for(int n=0;n<objects.size();n++){if(++scanned>256||result.size()>=8){status="Curtain discovery budget reached";return List.of();}var object=objects.get(n);if(object==null||object.getClass()!=IsoCurtain.class)continue;try{var curtain=(IsoCurtain)object;if(curtain.getSquare()!=square||curtain.getObjectIndex()<0||curtain.getSprite()==null||!curtain.canInteractWith(player))continue;var geometry=data(curtain.getSprite());if(geometry==null||geometry.north!=curtain.getNorth())continue;result.add(new Target(curtain,square,curtain.getSprite(),curtain.getObjectAttachedTo(),curtain.IsOpen(),base(curtain),geometry));}catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){/* Unsupported neighbors cannot replace a valid cloth target. */}}
   }status=result.isEmpty()?"No reachable rendered curtain cloth":"Exact curtain cloth ready: "+result.size();
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){status="Curtain geometry unavailable";return List.of();}return List.copyOf(result);
 }
 public boolean current(Target target,IsoPlayer actor){
  if(target==null||actor==null||actor.getSquare()==null||target.source.getClass()!=IsoCurtain.class||target.source.getSquare()!=target.square||target.source.getSprite()!=target.sprite||target.source.getObjectIndex()<0||target.source.IsOpen()!=target.open||target.source.getNorth()!=target.data.north||target.source.getObjectAttachedTo()!=target.attached||!target.source.canInteractWith(actor)||target.square.getObjects().indexOf(target.source)<0||base(target.source).distance(target.base)>.0001)return false;
  if(target.attached!=null&&target.attached.getObjectIndex()<0)return false;try{return provider.enabled()&&target.data.matches(provider.read(target.sprite));}catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){return false;}
 }
 public ContactFrame.Point touch(Target target,WorldRigFrame.Hand hand){if(target==null||hand==null)return null;var local=target.local(hand.grip());var closest=CurtainPolicy.closest(target.data.panel,local);return closest!=null&&closest.distance(local)<=.04*hand.units()&&CurtainPolicy.edge(target.data.panel,closest,hand.units())?target.world(closest):null;}
 /** Native tile blocking and exact current frame/glass triangles, never an aperture bounding box. */
 public boolean clearSegment(IsoPlayer player,ContactFrame.Point from,ContactFrame.Point to){
  if(player==null||player.getSquare()==null||from==null||to==null)return false;var here=player.getSquare();int scanned=0;
  try{for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){var square=player.getCell().getGridSquare(here.getX()+dx,here.getY()+dy,here.getZ());if(square==null)continue;var objects=square.getObjects();for(int n=0;n<objects.size();n++){if(++scanned>256)return false;var object=objects.get(n);if(!(object instanceof IsoWindow window)||window.getSquare()!=square||window.getObjectIndex()<0)continue;var r=provider.read(window.getSprite());if(r==null){if(possibleWindow(window,from,to))return false;continue;}var d=new NativeWindowGeometry.Data(r.north,r.plane,r.mesh,r.aperture,r.bounds);CollisionShape shape=NativeWindowGeometry.window(d,!window.IsOpen()&&!window.isDestroyed()&&!window.isGlassRemoved());var origin=base(window);if(NativeWindowGeometry.intersects(shape,from.x()-origin.x(),from.y()-origin.y(),from.height()-origin.height(),to.x()-origin.x(),to.y()-origin.y(),to.height()-origin.height()))return false;}}
   return true;
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){return false;}
 }
 // An unknown intersected native window blocks admission. It never creates a proxy or counts as ready geometry.
 private static boolean possibleWindow(IsoWindow window,ContactFrame.Point from,ContactFrame.Point to){var square=window.getSquare();boolean north=window.getNorth();double plane=north?square.getY():square.getX(),a=north?from.y():from.x(),b=north?to.y():to.x();if((a-plane)*(b-plane)>0||Math.abs(b-a)<1e-9)return false;double t=(plane-a)/(b-a);if(t<=0||t>=1)return false;double along=north?from.x()+(to.x()-from.x())*t:from.y()+(to.y()-from.y())*t,low=north?square.getX():square.getY(),height=from.height()+(to.height()-from.height())*t-square.getZ()*Math.sqrt(6);return along>=low-.05&&along<=low+1.05&&height>=0&&height<=2.5;}
}
