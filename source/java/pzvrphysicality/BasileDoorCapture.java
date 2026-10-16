package pzvrphysicality;
import java.lang.ref.WeakReference;import java.lang.reflect.*;import java.util.*;
import viewpoint.core.Frame;import zombie.iso.*;import zombie.iso.objects.*;import zombie.core.skinnedmodel.model.ModelMesh;
/** Passive effective-model lineage from the exact native drawer range. Never calls a provider factory. */
final class BasileDoorCapture {
 private static Field drawerModel,rawModel,rawSprite;private static final Map<Class<?>,Map<String,Field>> fields=new WeakHashMap<>();
 private static Field field(Class<?> type,String name)throws ReflectiveOperationException{var map=fields.computeIfAbsent(type,k->new HashMap<>());var f=map.get(name);if(f==null){f=type.getDeclaredField(name);f.setAccessible(true);map.put(name,f);}return f;}
 private static Object get(Object source,String name)throws ReflectiveOperationException{return field(source.getClass(),name).get(source);}
 private static Object staticGet(Class<?> type,String name)throws ReflectiveOperationException{return field(type,name).get(null);}
 private static final class Pose {
  final String name,texture,animation,runtime;final float time,scale;final float[] values;
  Pose(SpriteModel model){name=model.getModelScriptName();texture=model.getTextureName();animation=model.getAnimationName();runtime=model.getRuntimeString();time=model.getAnimationTime();scale=model.getScale();var t=model.getTranslate();var r=model.getRotate();values=new float[]{t.x,t.y,t.z,r.x,r.y,r.z};if(!Float.isFinite(time)||!Float.isFinite(scale)||scale<=0||scale>16)throw new IllegalArgumentException("Basile pose");for(float v:values)if(!Float.isFinite(v)||Math.abs(v)>3600)throw new IllegalArgumentException("Basile pose component");for(String s:List.of(Objects.toString(name,""),Objects.toString(texture,""),Objects.toString(animation,""),Objects.toString(runtime,"")))if(s.length()>512)throw new IllegalArgumentException("Basile metadata budget");}
  boolean matches(SpriteModel model){var p=new Pose(model);return Objects.equals(name,p.name)&&Objects.equals(texture,p.texture)&&Objects.equals(animation,p.animation)&&Objects.equals(runtime,p.runtime)&&time==p.time&&scale==p.scale&&Arrays.equals(values,p.values);}
 }
 static final class Receipt {
  final BasileDoorAssets.Source source;final WeakReference<IsoObject> owner;final WeakReference<SpriteModel> model,raw;final WeakReference<Object> sprite,initialized,entry;final Pose pose,rawPose;final WeakReference<Object> nativeModel;final WeakReference<ModelMesh> nativeMesh;final String key;final boolean explicitPose,hadRaw,hadInit;
  Receipt(BasileDoorAssets.Source source,IsoObject owner,SpriteModel model,Object entry,Object nativeModel,ModelMesh nativeMesh)throws ReflectiveOperationException{
   if(!entry.getClass().getName().equals("basile.doors3d.DoorCatalog$Entry"))throw new IllegalArgumentException("Basile entry type");if(nativeModel==null||nativeMesh==null)throw new IllegalArgumentException("Basile native drawer mesh");this.nativeModel=new WeakReference<>(nativeModel);this.nativeMesh=new WeakReference<>(nativeMesh);this.source=source;this.owner=new WeakReference<>(owner);this.model=new WeakReference<>(model);this.sprite=new WeakReference<>(owner.getSprite());this.entry=new WeakReference<>(entry);pose=new Pose(model);String name=(String)get(entry,"model");String texture=(String)get(entry,"texture");
   if(!Objects.equals(name,pose.name)||!Objects.equals(texture,pose.texture)||!name.startsWith("BasileDoors3D."))throw new IllegalArgumentException("Basile effective model");key=BasileDoorAssets.key("BasileDoors3D/"+name.substring(14));if(key==null)throw new IllegalArgumentException("Basile model asset");
   if(rawModel==null){rawModel=field(IsoObject.class,"spriteModel");rawSprite=field(IsoObject.class,"spriteModelInit");}var original=(SpriteModel)rawModel.get(owner);var init=rawSprite.get(owner);hadRaw=original!=null;hadInit=init!=null;raw=new WeakReference<>(original);initialized=new WeakReference<>(init);
   rawPose=original==null?null:new Pose(original);if(!mapping(entry,original))throw new IllegalArgumentException("Basile native selection predicate");boolean open=owner instanceof IsoDoor d?d.IsOpen():((IsoThumpable)owner).IsOpen();if(((Boolean)get(entry,"open"))!=open)throw new IllegalArgumentException("Basile open state");boolean missing=(Boolean)get(entry,"allowMissing");if(original==null?!missing:init!=owner.getSprite())throw new IllegalArgumentException("Basile native-cache lineage");
   Object authoredPose=get(entry,"pose");explicitPose=missing&&authoredPose!=null;if(explicitPose&&!explicit(authoredPose,pose))throw new IllegalArgumentException("Basile fixed pose mismatch");
   String standard="standard_door "+get(entry,"orientation")+" "+(open?"open":"closed");String expectedRuntime=explicitPose||rawPose==null||rawPose.runtime==null?standard:rawPose.runtime;if(!Objects.equals(expectedRuntime,pose.runtime)||!explicitPose&&(rawPose==null||rawPose.time!=pose.time||rawPose.scale!=pose.scale||!Objects.equals(rawPose.animation,pose.animation)||!Arrays.equals(rawPose.values,pose.values)))throw new IllegalArgumentException("Basile copied native pose mismatch");
  }
  boolean current(IsoObject actual){
   try{if(actual==null||nativeModel.get()==null||nativeMesh.get()==null||!nativeMesh.get().isReady()||get(nativeModel.get(),"mesh")!=nativeMesh.get()||owner.get()!=actual||source!=BasileDoorAssets.cached()||Boolean.getBoolean("basile.doors3d.disabled")||actual.getSprite()!=sprite.get()||actual.getSprite()==null||model.get()==null||entry.get()==null)return false;
    if(rawModel.get(actual)!=raw.get()||rawSprite.get(actual)!=initialized.get()||hadRaw&&raw.get()==null||hadInit&&initialized.get()==null||rawPose!=null&&!rawPose.matches(raw.get())||!mapping(entry.get(),raw.get()))return false;
    synchronized(source.provider){var clones=(Map<?,?>)staticGet(source.provider,"clones");var catalog=(Map<?,?>)staticGet(source.catalog,"ENTRIES");if(clones.size()>4096||catalog.size()!=396||clones.get(actual)!=model.get()||catalog.get(actual.getSpriteName())!=entry.get())return false;}
    if(actual instanceof IsoDoor d){if(d.isDestroyed())return false;}else if(!(actual instanceof IsoThumpable t)||!t.isDoor()||t.isDestroyed())return false;
    return pose.matches(model.get())&&(!explicitPose||explicit(get(entry.get(),"pose"),pose));
   }catch(ReflectiveOperationException|RuntimeException|LinkageError stale){return false;}
  }
  boolean mesh(ModelMesh mesh){return mesh!=null&&mesh==nativeMesh.get()&&Objects.equals(key,BasileDoorAssets.key(mesh.getPath().getPath()));}
 }
 private static boolean mapping(Object entry,SpriteModel raw)throws ReflectiveOperationException{
  if(entry==null)return false;if(raw==null)return (Boolean)get(entry,"allowMissing");Object runtime=get(entry,"runtime");if(runtime!=null)return runtime.equals(raw.getRuntimeString());Object model=get(entry,"sourceModel");return model!=null&&raw.getRuntimeString()==null&&model.equals(raw.getModelScriptName());
 }
 private static boolean explicit(Object source,Pose pose)throws ReflectiveOperationException{
  if(source==null||!source.getClass().getName().equals("basile.doors3d.DoorCatalog$Pose")||!Objects.equals(get(source,"animation"),pose.animation)||((Float)get(source,"time"))!=pose.time||((Float)get(source,"scale"))!=pose.scale)return false;String[] names={"x","y","z","rx","ry","rz"};for(int i=0;i<6;i++)if(((Float)get(source,names[i]))!=pose.values[i])return false;return true;
 }
 static Receipt read(Frame frame,int first,IsoObject owner){
  if(frame==null||owner==null||first<0||!Set.of("zombie.iso.objects.IsoDoor","zombie.iso.objects.IsoThumpable").contains(owner.getClass().getName()))return null;
  var source=BasileDoorAssets.cached();if(source==null||Boolean.getBoolean("basile.doors3d.disabled"))return null;
  try{var draws=frame.drawers;if(first>draws.size()||draws.size()-first>8)return null;SpriteModel effective=null;Object nativeModel=null;ModelMesh nativeMesh=null;int found=0;
   for(int i=first;i<draws.size();i++){var drawer=draws.get(i);if(drawer==null||!drawer.getClass().getName().equals("zombie.core.skinnedmodel.model.IsoObjectModelDrawer"))continue;if(drawerModel==null){var type=Class.forName("zombie.core.skinnedmodel.model.IsoObjectModelDrawer",false,BasileDoorCapture.class.getClassLoader());drawerModel=field(type,"spriteModel");}var model=(SpriteModel)drawerModel.get(drawer);if(model==null||model.getModelScriptName()==null||!model.getModelScriptName().startsWith("BasileDoors3D."))continue;effective=model;nativeModel=get(drawer,"model");nativeMesh=nativeModel==null?null:(ModelMesh)get(nativeModel,"mesh");found++;}
   if(found!=1)return null;Object entry;
   synchronized(source.provider){var clones=(Map<?,?>)staticGet(source.provider,"clones");var catalog=(Map<?,?>)staticGet(source.catalog,"ENTRIES");if(clones.size()>4096||catalog.size()!=396||clones.get(owner)!=effective)return null;entry=catalog.get(owner.getSpriteName());}
   if(entry==null)return null;var result=new Receipt(source,owner,effective,entry,nativeModel,nativeMesh);return result.current(owner)?result:null;
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){return null;}
 }
 /** A cached selected provider owner must never reuse a previous native leaf after a provider switch. */
 static boolean nativeOwner(IsoObject owner){var source=BasileDoorAssets.cached();if(source==null||Boolean.getBoolean("basile.doors3d.disabled"))return true;try{synchronized(source.provider){var entry=((Map<?,?>)staticGet(source.catalog,"ENTRIES")).get(owner.getSpriteName());if(rawModel==null){rawModel=field(IsoObject.class,"spriteModel");rawSprite=field(IsoObject.class,"spriteModelInit");}return entry==null||!mapping(entry,(SpriteModel)rawModel.get(owner))||!((Map<?,?>)staticGet(source.provider,"clones")).containsKey(owner);}}catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){return false;}}
 /** When a provider predicate stops matching, only a current raw-model drawer can restore native contact. */
 static boolean nativeDrawer(Frame frame,int first,IsoObject owner){
  var source=BasileDoorAssets.cached();if(source==null||Boolean.getBoolean("basile.doors3d.disabled"))return true;
  try{boolean relevant;synchronized(source.provider){relevant=((Map<?,?>)staticGet(source.catalog,"ENTRIES")).containsKey(owner.getSpriteName())||((Map<?,?>)staticGet(source.provider,"clones")).containsKey(owner);}if(!relevant)return true;
   if(rawModel==null){rawModel=field(IsoObject.class,"spriteModel");rawSprite=field(IsoObject.class,"spriteModelInit");}var raw=(SpriteModel)rawModel.get(owner);if(raw==null||rawSprite.get(owner)!=owner.getSprite()||first<0||first>frame.drawers.size()||frame.drawers.size()-first>8)return false;int count=0;
   for(int i=first;i<frame.drawers.size();i++){var drawer=frame.drawers.get(i);if(drawer!=null&&drawer.getClass().getName().equals("zombie.core.skinnedmodel.model.IsoObjectModelDrawer")){if(drawerModel==null)drawerModel=field(drawer.getClass(),"spriteModel");if(drawerModel.get(drawer)!=raw)return false;count++;}}return count==1;
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){return false;}
 }
 private BasileDoorCapture(){}
}
