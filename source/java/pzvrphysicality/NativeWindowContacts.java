package pzvrphysicality;

import java.lang.reflect.*;
import java.util.*;
import viewpoint.world.WorldMesher;
import viewpointvr.physics.CollisionShape;
import viewpointvr.physics.RigidPhysics;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoWindow;

/** Reads already-created selected window surfaces. Never requests asset construction or changes native state. */
public final class NativeWindowContacts {
 public static final String PROVIDER_SHA256="42f9aecafbb34a4bc76c6a8130720608fed79de9f78f179ef1fca9d4d635d6b1";
 private record Cached(Object surface,NativeWindowGeometry.Data data,CollisionShape frame,CollisionShape glazed){}
 private final Map<IsoWindow,Cached> cache=new WeakHashMap<>();
 private final Set<IsoObject> owned=Collections.newSetFromMap(new IdentityHashMap<>());
 private Thread thread;private IsoPlayer actor;private Class<?> api;private Method has;private Field surfaces,north,plane,mesh,aperture,bounds;
 private boolean unavailable;private int count,glazed,missing,skipped,rebuilds;
 private void access()throws Exception{
  if(api!=null||unavailable)return;
  try{
   var owner=Class.forName("techjungle.betterwindow.WindowAssets",false,NativeWindowContacts.class.getClassLoader());SdkPins.verify(owner,PROVIDER_SHA256);
   var surface=Class.forName("techjungle.betterwindow.Surface",false,owner.getClassLoader());
   var ready=owner.getMethod("hasSurface",zombie.iso.sprite.IsoSprite.class);if(ready.getReturnType()!=boolean.class||!Modifier.isStatic(ready.getModifiers()))throw new NoSuchMethodException("Window cache readiness");
   var source=owner.getDeclaredField("_surfaces");if(!Map.class.isAssignableFrom(source.getType())||!Modifier.isStatic(source.getModifiers()))throw new NoSuchFieldException("Window surface cache");source.setAccessible(true);
   north=surface.getField("north");plane=surface.getField("plane");mesh=surface.getField("mesh");aperture=surface.getField("aperture");bounds=surface.getField("bounds");
   if(north.getType()!=boolean.class||plane.getType()!=float.class||mesh.getType()!=float[].class||aperture.getType()!=float[].class||bounds.getType()!=float[].class)throw new NoSuchFieldException("Window surface layout");
   surfaces=source;has=ready;api=owner;
  }catch(Exception|LinkageError failure){unavailable=true;throw failure;}
 }
 private Object surface(IsoWindow window)throws ReflectiveOperationException{
  synchronized(api){if(!Boolean.TRUE.equals(has.invoke(null,window.getSprite())))return null;return ((Map<?,?>)surfaces.get(null)).get(window.getSprite());}
 }
 private static boolean glass(IsoWindow window){return !window.IsOpen()&&!window.isSmashed()&&!window.isGlassRemoved()&&!window.isDestroyed();}
 public List<PhysicalWorld.Static> sample(IsoPlayer player){
  if(thread!=null&&thread!=Thread.currentThread())return List.of();
  if(thread==null)thread=Thread.currentThread();owned.clear();count=glazed=missing=skipped=rebuilds=0;
  if(player!=actor){cache.clear();actor=player;}
  if(player==null||player.isDead()||player.getCell()==null||player.getSquare()==null||zombie.network.GameClient.client||zombie.network.GameServer.server){cache.clear();return List.of();}
  try{access();}catch(Exception|LinkageError unsupported){cache.clear();skipped++;return List.of();}
  if(api==null)return List.of();
  var result=new ArrayList<PhysicalWorld.Static>();var visited=Collections.newSetFromMap(new IdentityHashMap<IsoWindow,Boolean>());
  int x=(int)Math.floor(player.getX()),y=(int)Math.floor(player.getY()),z=(int)Math.floor(player.getZ()),scanned=0,triangles=0;
  outer:for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++){
   var square=player.getCell().getGridSquare(x+dx,y+dy,z);if(square==null)continue;var objects=square.getObjects();
   for(int index=0;index<objects.size();index++){
    if(++scanned>512||result.size()>=32)break outer;
    if(!(objects.get(index) instanceof IsoWindow window)||!visited.add(window)||window.getSquare()!=square||window.getObjectIndex()<0||window.getSprite()==null)continue;
    try{
     var actual=surface(window);if(actual==null){cache.remove(window);missing++;continue;}
     // This native producer replaces the generic packed window. Omit a failed replacement rather than add its stale packed collider.
     owned.add(window);
     boolean n=north.getBoolean(actual);float p=plane.getFloat(actual);var m=(float[])mesh.get(actual);var a=(float[])aperture.get(actual);var b=(float[])bounds.get(actual);
     if(n!=window.getNorth())throw new IllegalArgumentException("Window orientation changed");
     var old=cache.get(window);
     if(old==null||old.surface!=actual||!old.data.matches(n,p,m,a,b)){
      var data=new NativeWindowGeometry.Data(n,p,m,a,b);old=new Cached(actual,data,NativeWindowGeometry.window(data,false),NativeWindowGeometry.window(data,true));
      if(cache.size()>=64&&!cache.containsKey(window))cache.clear();cache.put(window,old);rebuilds++;
     }
     boolean closed=glass(window);var shape=closed?old.glazed:old.frame;if(shape==null){missing++;continue;}
     int faces=shape.faces().length/3;if(triangles+faces>24000){skipped++;continue;}
     float rise=WorldMesher.rise(window);if(!Float.isFinite(rise)||Math.abs(rise)>.5f)throw new IllegalArgumentException("Window rise");
     result.add(new PhysicalWorld.Static("window:"+square.getX()+":"+square.getY()+":"+z+":"+index,shape,RigidPhysics.pose(square.getX()+.5f,square.getY()+.5f,(float)(square.getZ()*Math.sqrt(6))+rise)));
     count++;if(closed)glazed++;triangles+=faces;
    }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){cache.remove(window);skipped++;}
   }
  }
  cache.keySet().retainAll(visited);return result;
 }
 public boolean owns(IsoObject object){return owned.contains(object);}
 public String status(){return "window contacts "+count+", closed glass "+glazed+", unavailable "+missing+", skipped "+skipped+", rebuilt "+rebuilds;}
 public void clear(){if(thread!=null&&thread!=Thread.currentThread())return;cache.clear();owned.clear();actor=null;count=glazed=missing=skipped=rebuilds=0;}
}
