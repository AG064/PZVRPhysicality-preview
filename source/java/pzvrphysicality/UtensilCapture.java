package pzvrphysicality;

import java.lang.ref.WeakReference;import java.lang.reflect.Field;import java.util.*;
import local.pzvr.*;import viewpoint.core.Frame;import viewpoint.render.ModelDraws;
import zombie.characters.IsoPlayer;import zombie.inventory.InventoryItem;import zombie.core.skinnedmodel.model.*;

/** Reads the final PZVR-adjusted rigid records. It never changes a packet, equipped slot or native model. */
public final class UtensilCapture {
 private record Profile(String item,String model,String geometry,String mirrored,MouthMath.Vec tip,UtensilContact.Region region,String edible,String mirroredEdible,boolean spoonOnly){
  Profile(String item,String model,String geometry,String mirrored,MouthMath.Vec tip){this(item,model,geometry,mirrored,tip,UtensilContact.Region.WHOLE,geometry,mirrored,false);}
 }
 private static Profile bowl(String item){return new Profile(item,"BowlSoup_Ground","b5793003c706141595962ee86e77a1b301aa848aef6fb6a04cafd51522a1b424","7dc495caa320b55f632f848e1f03083376b2bf6e3be723d6b90fdc0d56b07abc",null,UtensilContact.Region.SOUP_SURFACE,"6f52da8299d2643bbd73f56d2f3b6454661f1ae366ae15191a6a9e333c56c1ef","50f438a9181126a7fcae0e0f352a86b3936abe7d66192055e41380945e125ab8",true);}
 private static final List<Profile> TOOLS=List.of(
  new Profile("Base.Spoon","Spoon","1a68441c43ee807dcec28465c2b3d52fb48a1440cb9097ce69e3cb3dd46e0803","6cb2f4ef7336bd98b604953d7d4fb46bf8bda1b115d84cfc05610b1a69cc1835",new MouthMath.Vec(.000084,.162139,0)),
  new Profile("Base.Fork","Fork","127979cdab8754b0514282976f2fcc7ff2b01ab028f0d293e5f34e44131ce4f2","ba2afa1fbe595e573bc636bb63e3f54e79a50dff8e0056102e23b78c1c2347dd",new MouthMath.Vec(-.004137,.162496,-.000016)));
 private static final List<Profile> FOODS=List.of(
  new Profile("Base.Bread","Bread","c539b1163e1db01bfc6c5a1cb6bdecff300e1a59161fd0ec547012c9ab109f89","adf66d6f22f29567bcbebea2f49da8b24d8f8631d011216b46bf9d6136cd9232",null),
  new Profile("Base.BreadSlices","BreadSlices","31bdef053536889d7cd12055b42e1907fd050f568c42c29af172568a90fdac19","37ecfe6d7f5dec9a6e63843f0394c48298bf4a410035d6daf629f22c7df29e39",null),
  new Profile("Base.Apple","Apple_Ground","547d661741f39d886b202406edff7ee06ed1d69faf0f140b3446ef7fe311caa9","e637a8bf2bc169bf0b3c55d317a0fc58a9e8edcc81124d108ad3aff8ef2c8f77",null,UtensilContact.Region.LARGEST_COMPONENT,"5b0bacf4cca451678c35fc9a32586624055298bf45cd994c9bc017e46d090198","6930176e2e56125d5b03a52bc46430146161d19a9999dc14febd73f335e55236",false),
  new Profile("Base.Pear","Pear_Ground","4b7b7b0ab06f7dc91eaafd3593d9c77a63d184fdab3d4e484af266cf89c8d88c","cf9da434c8e1ed6ac1b32aa2a685c5fc648e14809987eccf3e32155903d1aade",null,UtensilContact.Region.LARGEST_COMPONENT,"47cb0423a0490c626025e7d44fd819fd0e3bcd944c3c1d60c5129e0ec27cfe2d","d682f91ad99f2a77765244dc8ed9583cc93616e0e286de0ea6c900b5ff9e652f",false),
  new Profile("Base.Cheese","Cheese","fdea4613283ab2d3324a8b6475eea2344422488a87c3358349d0cdd0dbf86124","a9c9209c272c47aaf6b10318a523de6efd8e1f748049351c2be8dd645e536a39",null,UtensilContact.Region.CHEESE_CUT,"aebcb3c1a9e3bf8d106e8cf6d6fe05fafeb6420df220d6ecbb7d45322d4aaf07","ed72de209e972eea4321b7ea234e23b9cfac679f19ab000c28371db71781e887",false),
  new Profile("Base.Carrots","Carrots_Hand","c11cc0158b51b62eecef9acdea3ec1092b5b42b76e5468dbb7658c8dd7b711bb","382594113ee9d316ec5c9056ed5ca208c413f1f5e0d256ba69c95a292d6592fd",null,UtensilContact.Region.CARROT_ROOT,"77bf885c44b82d82af97ffa77f0e8bd6d6eadd7b31ee6a89c533def3d1ca1983","d897c132a1b6207fc785c48f7e621eecf8c3028332e1fe0ec7c6f653cab521d0",false),
  bowl("Base.SoupBowl"),bowl("Base.SoupBowlClay"),bowl("Base.StewBowl"),bowl("Base.StewBowlClay"));
 private record Edible(UtensilContact.Geometry geometry,String fingerprint){}
 private record Mesh(UtensilContact.Geometry geometry,String fingerprint,ItemMeshCpu.Data source,Map<UtensilContact.Region,Edible> regions){}
 private record Access(Field meshes,Field instances,Field values){}
 private record Record(ModelMesh mesh,float[] matrix){}
 private record View(WeakReference<IsoPlayer> actor,WeakReference<InventoryItem> tool,WeakReference<InventoryItem> food,WeakReference<ModelInstance> toolInstance,WeakReference<ModelInstance> foodInstance,WeakReference<ModelMesh> toolMesh,WeakReference<ModelMesh> foodMesh,WeakReference<Object> scene,int side,long epoch,long rigGeneration,long inputTime,long time,double units,MouthMath.Vec tipWorld,MouthMath.Vec tipLocal,MouthMath.Vec foodContactLocal,float[] toolMatrix,float[] foodMatrix,int[] edibleFaces,ItemMeshCpu.Data toolGeometry,ItemMeshCpu.Data foodGeometry,String toolFingerprint,String foodFingerprint,boolean foodContact,boolean mouthContact){}
 private record Loaded(WeakReference<UtensilPolicy.Bite<InventoryItem>> bite,WeakReference<IsoPlayer> actor,MouthMath.Vec foodContactLocal,String foodFingerprint,String toolFingerprint,ItemMeshCpu.Data toolGeometry,ItemMeshCpu.Data foodGeometry,float[] toolMatrix,float[] foodMatrix,MouthMath.Vec tipLocal){Loaded{toolMatrix=toolMatrix.clone();foodMatrix=foodMatrix.clone();}}
 public record Sample(boolean foodContact,boolean mouthContact){}
 /** Native handles are valid for this draw only; all geometry coordinates and matrices are detached. */
 public record Visual(int side,Object scene,long epoch,long rigGeneration,long inputTime,long hostTime,double units,MouthMath.Vec tipWorld,MouthMath.Vec tipLocal,MouthMath.Vec foodContactLocal,float[] toolMatrix,float[] foodMatrix,float[] loadedToolMatrix,float[] loadedFoodMatrix,ModelInstance foodModelInstance,ModelMesh foodMesh,ItemMeshCpu.Data foodGeometry,int[] edibleFaces){
  public Visual{toolMatrix=toolMatrix.clone();foodMatrix=foodMatrix.clone();loadedToolMatrix=loadedToolMatrix.clone();loadedFoodMatrix=loadedFoodMatrix.clone();edibleFaces=edibleFaces.clone();}
  public float[] toolMatrix(){return toolMatrix.clone();}public float[] foodMatrix(){return foodMatrix.clone();}public float[] loadedToolMatrix(){return loadedToolMatrix.clone();}public float[] loadedFoodMatrix(){return loadedFoodMatrix.clone();}public int[] edibleFaces(){return edibleFaces.clone();}
 }
 private static final WeakHashMap<ModelMesh,Mesh> geometry=new WeakHashMap<>();
 private static volatile List<View> views=List.of();private static volatile Loaded loaded;private static Access access;private static String status="Utensil geometry Off";
 private static Profile profile(List<Profile> profiles,InventoryItem item){return item==null?null:profiles.stream().filter(p->p.item.equals(item.getFullType())).findFirst().orElse(null);}
 public static boolean supportedTool(InventoryItem item){return profile(TOOLS,item)!=null;}
 public static boolean supportedFood(InventoryItem item){return profile(FOODS,item)!=null;}
 public static boolean supportedPair(InventoryItem tool,InventoryItem food){var source=profile(FOODS,food);return supportedTool(tool)&&source!=null&&(!source.spoonOnly||"Base.Spoon".equals(tool.getFullType())&&"2handbowl".equals(food.getEatType()));}
 private static MouthMath.Vec point(ContactFrame.Point p){return new MouthMath.Vec(p.x(),p.y(),p.height());}
 private static Access access()throws ReflectiveOperationException{if(access==null)access=new Access(field("meshes"),field("instances"),field("values"));return access;}
 private static Field field(String name)throws ReflectiveOperationException{var field=ModelDraws.class.getDeclaredField(name);field.setAccessible(true);return field;}
 private static Record record(Frame frame,ModelInstance instance,Profile profile)throws ReflectiveOperationException{
  if(instance==null||instance.modelScript==null||!profile.model.equals(instance.modelScript.getName()))throw new IllegalArgumentException("Unverified held model identity");
  var fields=access();var draws=frame.scene.models;int count=draws.count();
  var meshes=(ModelMesh[])fields.meshes.get(draws);var instances=(ModelInstance[])fields.instances.get(draws);var values=(float[])fields.values.get(draws);
  if(count<0||count>4096||count>meshes.length||count>instances.length||count>values.length/68)throw new IllegalArgumentException("Held record budget");
  int index=-1;for(int n=0;n<count;n++)if(instances[n]==instance){if(index>=0)throw new IllegalArgumentException("Ambiguous held model record");index=n;}
  if(index<0||meshes[index]==null||!meshes[index].isReady()||meshes[index].skinningData!=null||instance.model==null||instance.model.mesh!=meshes[index])throw new IllegalArgumentException("Original ready rigid held record unavailable");
  return new Record(meshes[index],Arrays.copyOfRange(values,index*68,index*68+16));
 }
 private static synchronized Mesh mesh(ModelMesh mesh)throws ReflectiveOperationException{
  var cached=geometry.get(mesh);var retained=ItemMeshCpu.get(mesh);var software=mesh.softwareMesh;
  if(cached!=null&&retained!=null&&cached.source==retained)return cached;
  float[] positions;int[] faces;
  if(retained!=null){positions=retained.vertices();faces=retained.faces();}
  else if(software!=null&&software.verticesUnskinned!=null&&software.indicesUnskinned!=null){
   // Software arrays are mutable. Re-read the small supported profiles rather than retaining an old fingerprint.
   if(software.verticesUnskinned.length<3||software.verticesUnskinned.length>512||software.indicesUnskinned.length>1536)throw new IllegalArgumentException("Held software geometry profile budget");
   positions=new float[software.verticesUnskinned.length*3];faces=software.indicesUnskinned.clone();
   for(int n=0;n<software.verticesUnskinned.length;n++){var vertex=software.verticesUnskinned[n];if(vertex==null||vertex.position==null)throw new IllegalArgumentException("Held CPU vertex unavailable");positions[n*3]=vertex.position.x();positions[n*3+1]=vertex.position.y();positions[n*3+2]=vertex.position.z();}
  }else throw new IllegalArgumentException("Held CPU geometry unavailable; preloaded models need a future native capture");
  var copy=new UtensilContact.Geometry(positions,faces);cached=new Mesh(copy,UtensilContact.fingerprint(copy),retained,new EnumMap<>(UtensilContact.Region.class));
  if(geometry.size()>=4&&!geometry.containsKey(mesh))geometry.clear();geometry.put(mesh,cached);return cached;
 }
 private static boolean mirrored(Profile profile,Mesh mesh){if(profile.geometry.equals(mesh.fingerprint))return false;if(profile.mirrored.equals(mesh.fingerprint))return true;throw new IllegalArgumentException("Held geometry differs from the verified utensil/edible profile");}
 private static synchronized Edible edible(Profile profile,Mesh mesh,boolean mirrored){var result=mesh.regions.get(profile.region);if(result==null){var region=UtensilContact.region(mesh.geometry,profile.region);result=new Edible(region,UtensilContact.fingerprint(region));mesh.regions.put(profile.region,result);}if(!(mirrored?profile.mirroredEdible:profile.edible).equals(result.fingerprint))throw new IllegalArgumentException("Edible triangle role differs from its verified profile");return result;}
 private static MouthMath.Vec difference(MouthMath.Vec a,MouthMath.Vec b){return new MouthMath.Vec(a.x()-b.x(),a.y()-b.y(),a.z()-b.z());}
 private static void noncollapsed(Record record,MouthMath.Vec camera){
  var origin=UtensilContact.world(new MouthMath.Vec(0,0,0),record.matrix,camera);
  var x=difference(UtensilContact.world(new MouthMath.Vec(1,0,0),record.matrix,camera),origin);
  var y=difference(UtensilContact.world(new MouthMath.Vec(0,1,0),record.matrix,camera),origin);
  var z=difference(UtensilContact.world(new MouthMath.Vec(0,0,1),record.matrix,camera),origin);
  double product=x.distance(new MouthMath.Vec(0,0,0))*y.distance(new MouthMath.Vec(0,0,0))*z.distance(new MouthMath.Vec(0,0,0));
  double volume=x.x()*(y.y()*z.z()-y.z()*z.y())-x.y()*(y.x()*z.z()-y.z()*z.x())+x.z()*(y.x()*z.y()-y.y()*z.x());
  if(product<=1e-18||Math.abs(volume)<product*.0001)throw new IllegalArgumentException("Held geometry transform is collapsed");
 }
 private static MouthMath.Vec tip(Profile profile,Mesh mesh,boolean mirror){
  var expected=new MouthMath.Vec(profile.tip.x(),profile.tip.y(),mirror?-profile.tip.z():profile.tip.z());var vertices=mesh.geometry.vertices();MouthMath.Vec closest=null;double best=Double.POSITIVE_INFINITY;
  for(int n=0;n<vertices.length;n+=3)if(Math.abs(vertices[n+1]-expected.y())<.00001){var point=new MouthMath.Vec(vertices[n],vertices[n+1],vertices[n+2]);double distance=point.distance(expected);if(distance<best){closest=point;best=distance;}}
  if(closest==null||best>.02)throw new IllegalArgumentException("Verified working-end vertex unavailable");return closest;
 }
 private static boolean soupContact(UtensilContact.Surface surface,MouthMath.Vec tip,Record food,MouthMath.Vec camera,boolean mirrored,double units){
  var origin=UtensilContact.world(new MouthMath.Vec(0,0,0),food.matrix,camera);var opening=difference(UtensilContact.world(new MouthMath.Vec(0,0,mirrored?1:-1),food.matrix,camera),origin);double length=opening.distance(new MouthMath.Vec(0,0,0));
  if(length<=1e-12||opening.z()/length<.5)return false;var approach=difference(tip,surface.world());double signed=(approach.x()*opening.x()+approach.y()*opening.y()+approach.z()*opening.z())/length;
  return signed>=-units*.001;
 }
 private static boolean samePreview(IsoPlayer p){var actions=p.getCharacterActions();if(actions==null||actions.isEmpty())return true;var action=actions.get(0);return action!=null&&(!action.overrideHandModels||action.getPrimaryHandItem()==p.getPrimaryHandItem()&&action.getSecondaryHandItem()==p.getSecondaryHandItem());}
 public static void capture(Frame frame,IsoPlayer p){
  if(!NativeConsumption.enabled())return;
  try{
   if(!Main.ready()||frame==null||p==null||IsoPlayer.players.length==0||p!=IsoPlayer.players[0]||!samePreview(p)){views=List.of();status="Utensil capture waiting for matching native held identities";return;}
   long now=System.nanoTime();var rig=WorldRigCapture.forScene(frame.scene,now);
   if(rig==null||rig.actor()!=p||!rig.fresh(now)){views=List.of();status="Utensil capture waiting for a fresh tracked rig";return;}
   var fresh=new ArrayList<View>();String unavailable="Hold a verified Spoon/Fork and supported bare food, or a Spoon and soup/stew bowl, in opposite hands";
   for(int side=0;side<2;side++){
    var tool=NativeConsumption.hand(p,side);var food=NativeConsumption.hand(p,1-side);var toolProfile=profile(TOOLS,tool);var foodProfile=profile(FOODS,food);
    if(toolProfile==null)continue;if(foodProfile==null){unavailable="Utensils require a verified edible region; unsupported bowls, plates, pots and wrappers are unavailable";continue;}
    if(!NativeConsumption.utensilEligible(p,tool,food,side))continue;
    var hand=rig.hand(side);if(hand==null||rig.hand(1-side)==null)continue;
    var toolInstance=side==1?p.primaryHandModel:p.secondaryHandModel;var foodInstance=side==1?p.secondaryHandModel:p.primaryHandModel;
    var toolRecord=record(frame,toolInstance,toolProfile);var foodRecord=record(frame,foodInstance,foodProfile);
    var toolMesh=mesh(toolRecord.mesh);var foodMesh=mesh(foodRecord.mesh);boolean mirror=mirrored(toolProfile,toolMesh),foodMirror=mirrored(foodProfile,foodMesh);var edible=edible(foodProfile,foodMesh,foodMirror);
    noncollapsed(toolRecord,point(rig.camera()));noncollapsed(foodRecord,point(rig.camera()));
    var local=tip(toolProfile,toolMesh,mirror);var tip=UtensilContact.world(local,toolRecord.matrix,point(rig.camera()));
    double span=tip.distance(UtensilContact.world(new MouthMath.Vec(0,0,0),toolRecord.matrix,point(rig.camera())))/hand.units();
    if(span<.02||span>.5)throw new IllegalArgumentException("Held utensil is outside its supported physical extent");
    if(tip.distance(point(hand.grip()))>hand.units()*.5)throw new IllegalArgumentException("Held tip exceeds verified physical reach");
    var surface=UtensilContact.closest(tip,edible.geometry,foodRecord.matrix,point(rig.camera()));var originalSurface=foodProfile.region==UtensilContact.Region.WHOLE?surface:UtensilContact.closest(tip,foodMesh.geometry,foodRecord.matrix,point(rig.camera()));
    boolean contact=surface!=null&&surface.distance()<=hand.units()*.012&&originalSurface!=null&&surface.distance()<=originalSurface.distance()+hand.units()*.0001;
    if(contact&&foodProfile.spoonOnly)contact=soupContact(surface,tip,foodRecord,point(rig.camera()),foodMirror,hand.units());
    boolean mouth=tip.distance(point(rig.mouth()))<=hand.units()*NativeConsumption.mouthRadius();
    fresh.add(new View(new WeakReference<>(p),new WeakReference<>(tool),new WeakReference<>(food),new WeakReference<>(toolInstance),new WeakReference<>(foodInstance),new WeakReference<>(toolRecord.mesh),new WeakReference<>(foodRecord.mesh),new WeakReference<>(frame.scene),side,NativeConsumption.generation(),rig.generation(),rig.inputTime(),now,hand.units(),tip,local,surface==null?null:surface.local(),toolRecord.matrix,foodRecord.matrix,edible.geometry.faces(),toolMesh.source,foodMesh.source,toolMesh.fingerprint,foodMesh.fingerprint,contact,mouth));
   }
   views=List.copyOf(fresh);status=fresh.isEmpty()?unavailable:"Verified native utensil tip and edible triangle region available";
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){views=List.of();status="Utensil geometry unavailable: "+unsupported.getMessage();}
 }
 private static boolean current(ModelInstance instance,ModelMesh source,ItemMeshCpu.Data generation,String fingerprint){
  try{
   if(instance==null||source==null||!source.isReady()||source.skinningData!=null||instance.model==null||instance.model.mesh!=source||ItemMeshCpu.get(source)!=generation)return false;
   var actual=mesh(source);if(actual.source!=generation||!actual.fingerprint.equals(fingerprint))return false;
   return source.isReady()&&source.skinningData==null&&instance.model!=null&&instance.model.mesh==source&&ItemMeshCpu.get(source)==generation;
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){return false;}
 }
 private static View view(IsoPlayer actor,int side,InventoryItem tool,InventoryItem food,long now){
  var input=Engine.latestInput;var rig=WorldRigCapture.latest;
  if(!NativeConsumption.enabled()||input==null||!input.focused()||rig==null||rig.actor()!=actor||!rig.fresh(now))return null;
  var hand=side==0?input.left():input.right();var source=side==0?input.right():input.left();
  if(hand==null||source==null||!hand.tracked()||!source.tracked()||hand.grip()==null||source.grip()==null)return null;
  for(var view:views)if(view.actor.get()==actor&&view.side==side&&view.tool.get()==tool&&view.food.get()==food&&view.epoch==NativeConsumption.generation()
   &&view.toolInstance.get()==(side==1?actor.primaryHandModel:actor.secondaryHandModel)&&view.foodInstance.get()==(side==1?actor.secondaryHandModel:actor.primaryHandModel)
   &&view.rigGeneration==rig.generation()&&now>=view.time&&now-view.time<150_000_000L&&input.time()>=view.inputTime&&input.time()-view.inputTime<=150_000_000L){
    if(!current(view.toolInstance.get(),view.toolMesh.get(),view.toolGeometry,view.toolFingerprint)||!current(view.foodInstance.get(),view.foodMesh.get(),view.foodGeometry,view.foodFingerprint))return null;
    var reservation=loaded;var bite=NativeConsumption.bite();if(reservation!=null&&reservation.bite.get()==bite&&bite!=null&&bite.side()==side&&(reservation.toolGeometry!=view.toolGeometry||reservation.foodGeometry!=view.foodGeometry||!reservation.toolFingerprint.equals(view.toolFingerprint)||!reservation.foodFingerprint.equals(view.foodFingerprint)))return null;return view;
   }
  return null;
 }
 public static Sample sample(IsoPlayer actor,int side,InventoryItem tool,InventoryItem food,long now){var view=view(actor,side,tool,food,now);return view==null?null:new Sample(view.foodContact,view.mouthContact);}
 static boolean loaded(IsoPlayer actor,UtensilPolicy.Bite<InventoryItem> bite,long now){var view=view(actor,bite.side(),bite.tool(),bite.food(),now);if(view==null||!view.foodContact||view.foodContactLocal==null)return false;loaded=new Loaded(new WeakReference<>(bite),new WeakReference<>(actor),view.foodContactLocal,view.foodFingerprint,view.toolFingerprint,view.toolGeometry,view.foodGeometry,view.toolMatrix,view.foodMatrix,view.tipLocal);return true;}
 public static Visual visual(Frame frame,IsoPlayer actor,UtensilPolicy.Bite<InventoryItem> bite,long now){return frame==null?null:visual(frame.scene,actor,bite,now);}
 public static Visual visual(viewpoint.render.SceneData scene,IsoPlayer actor,UtensilPolicy.Bite<InventoryItem> bite,long now){
  var reservation=loaded;if(scene==null||actor==null||bite==null||NativeConsumption.bite()!=bite||reservation==null||reservation.bite.get()!=bite||reservation.actor.get()!=actor||!NativeConsumption.utensilEligible(actor,bite.tool(),bite.food(),bite.side()))return null;
  if(!(bite.food() instanceof zombie.inventory.types.Food food)||Double.doubleToLongBits(bite.hunger())!=Double.doubleToLongBits(food.getHungChange())||Double.doubleToLongBits(bite.baseHunger())!=Double.doubleToLongBits(food.getBaseHunger()))return null;
  var view=view(actor,bite.side(),bite.tool(),bite.food(),now);if(view==null||view.scene.get()!=scene||!reservation.foodFingerprint.equals(view.foodFingerprint))return null;var mesh=view.foodMesh.get();var instance=view.foodInstance.get();if(mesh==null||instance==null||!mesh.isReady()||mesh.skinningData!=null)return null;
  return new Visual(view.side,scene,view.epoch,view.rigGeneration,view.inputTime,view.time,view.units,view.tipWorld,reservation.tipLocal,reservation.foodContactLocal,view.toolMatrix,view.foodMatrix,reservation.toolMatrix,reservation.foodMatrix,instance,mesh,view.foodGeometry,view.edibleFaces);
 }
 public static synchronized void clear(){views=List.of();loaded=null;geometry.clear();status="Utensil geometry Off";}
 public static String status(){return status;}
 private UtensilCapture(){}
}
