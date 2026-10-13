package pzvrphysicality;
import java.lang.reflect.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import se.krka.kahlua.vm.KahluaTable;
import viewpoint.core.Frame;
import viewpoint.models.Models;
import viewpoint.packs.*;
import viewpoint.render.*;
import viewpoint.world.*;
import viewpointvr.physics.PackedModelGeometry;
import viewpointvr.physics.RigidPhysics;
import viewpointvr.physics.CollisionShape;
import zombie.characters.IsoPlayer;
import zombie.inventory.*;
import zombie.iso.*;
import zombie.core.skinnedmodel.model.*;

/** Original inventory items rendered through the audited native producer, with no cloned world objects. */
public final class NativeStorage {
 private static final int MAX_ITEMS=2048,MAX_DRAWS=64,MAX_RECORDS=256,STRIDE=68;
 private record Row(InventoryItem item,int slot,double x,double y,double z,float qx,float qy,float qz,float qw,boolean automatic,boolean raw,long revision){}
 private record Selection(ItemContainer container,String key,List<Row> rows,Object actor,Thread owner,long time){Selection{rows=List.copyOf(rows);}}
 public record Contact(InventoryItem item,ItemContainer container,IsoGridSquare square,StorageFrame.Bounds bounds,long time){}
 public record Destination(ItemContainer container,IsoGridSquare square,String key,float[] normalized){public Destination{normalized=normalized.clone();}public float[] normalized(){return normalized.clone();}}
 private record Access(Field flag,Field sink,Field values,Field meshes,Field flags,Field palette,Field shadow,Field count,List<Field> refs,Method own){}
 private static volatile Selection selected;private static volatile List<Contact> contacts=List.of();private static Access access;
 private static long warning;private static String status="Storage item models Off";
 private static final WeakHashMap<InventoryItem,Long> retry=new WeakHashMap<>();
 private static IsoObject fittedOwner;private static PackModel fittedModel;private static PackedModelGeometry.Mesh fittedMesh;private static StorageFrame fitted;private static StorageOcclusion occlusion;
 private static float fittedCos,fittedSin,fittedScale,fittedX,fittedY,fittedH;
 private record Located(Row row,ContactFrame.Point point){}
 private static List<Row> plannedRows;private static StorageFrame plannedFrame;private static List<Located> located=List.of();private static List<Row> visible=List.of();private static long nextPlan;
 private record Solved(StorageFrame frame,List<ModelMesh> meshes,long[] signature,ContactFrame.Point position){}
 private static final WeakHashMap<InventoryItem,Solved> solved=new WeakHashMap<>();private static long fittingQueries;
 private record PackedSolution(StorageFrame frame,StorageFrame.Bounds bounds,ContactFrame.Point pivot,List<StorageFrame.Bounds> occupied,ContactFrame.Point position){}
 private static final WeakHashMap<InventoryItem,PackedSolution> packed=new WeakHashMap<>();private static long packingQueries;
 private static boolean physicsEnabled;private static StorageBodies<InventoryItem> bodies;private static ItemContainer bodyContainer;private static String bodyKey;private static Object bodyActor;private static StorageFrame bodyFrame;
 private static long nextSave;private static final WeakHashMap<InventoryItem,float[]> released=new WeakHashMap<>();
 private static List<Row> residentPlan;
 private static final WeakHashMap<InventoryItem,Long> geometryRetry=new WeakHashMap<>();private static int missingPhysics;
 private static double number(KahluaTable row,String field){Object value=row.rawget(field);if(!(value instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IllegalArgumentException("Storage row number");return n.doubleValue();}
 private static int integer(Object value){if(!(value instanceof Number n)||!Double.isFinite(n.doubleValue())||n.doubleValue()!=n.intValue())throw new IllegalArgumentException("Storage row identity");return n.intValue();}
 private static boolean eligible(ItemContainer container){
  var player=IsoPlayer.players.length==0?null:IsoPlayer.players[0];var parent=container==null?null:container.getParent();var square=parent==null?null:parent.getSquare();
  return Main.ready()&&!zombie.network.GameServer.server&&player!=null&&!player.isDead()&&player.getVehicle()==null&&player.getSquare()!=null&&square!=null
   &&container.getContainingItem()==null&&container.isExplored()&&parent.getContainerIndex(container)>=0&&square.getObjects().contains(parent)
   &&player.getCell().getGridSquare(square.getX(),square.getY(),square.getZ())==square
   &&square.isAdjacentTo(player.getSquare())&&!square.isBlockedTo(player.getSquare())&&zombie.iso.areas.SafeHouse.isSafehouseAllowLoot(square,player);
 }
 public static void clear(){closeBodies();ShelfBridge.clear();selected=null;contacts=List.of();retry.clear();solved.clear();packed.clear();released.clear();geometryRetry.clear();fittingQueries=packingQueries=0;fitted=null;occlusion=null;fittedOwner=null;fittedModel=null;fittedMesh=null;plannedRows=null;plannedFrame=null;located=List.of();visible=List.of();nextPlan=0;status="Storage item models Off";}
 public static void physicsConfigure(boolean enabled)throws Exception{if(enabled)RigidPhysics.loadDefault();if(physicsEnabled!=enabled){closeBodies();physicsEnabled=enabled;}}
 private static void saveBodies(){
  if(bodies==null||bodyContainer==null||bodyFrame==null||zombie.network.GameClient.client||zombie.network.GameServer.server||!eligible(bodyContainer)||bodyActor!=IsoPlayer.players[0])return;
  var view=selected;if(view==null||view.container!=bodyContainer||!view.key.equals(bodyKey))return;
  for(var row:view.rows){var sample=bodies.sample(row.item);if(sample==null||row.item.getContainer()!=bodyContainer)continue;var norm=StoragePose.normalized(bodyFrame,sample.state());if(norm==null)continue;
   long revision=StorageStateSave.commit(row.item,bodyContainer,bodyKey,sample.revision(),norm);if(revision>0)bodies.revision(row.item,sample.revision(),revision);
  }
 }
 private static void closeBodies(){if(bodies!=null){try{saveBodies();}finally{bodies.close();bodies=null;}}bodyContainer=null;bodyActor=null;bodyFrame=null;bodyKey=null;residentPlan=null;nextSave=0;}
 private static void retireCapture(String reason){try{closeBodies();}finally{ShelfBridge.clear();contacts=List.of();packed.clear();fitted=null;occlusion=null;fittedOwner=null;fittedModel=null;fittedMesh=null;plannedRows=null;plannedFrame=null;located=List.of();visible=List.of();nextPlan=0;status=reason;}}
 private static void prepareBodies(Selection view,StorageFrame frame){
  if(!physicsEnabled||zombie.network.GameClient.client||zombie.network.GameServer.server||ShelfBridge.supported(view.container.getParent())&&!ShelfBridge.ready(view.container.getParent())){closeBodies();return;}
  if(bodies==null||bodyContainer!=view.container||!view.key.equals(bodyKey)||bodyActor!=view.actor){closeBodies();bodies=new StorageBodies<>();bodyContainer=view.container;bodyKey=view.key;bodyActor=view.actor;}
  if(bodyFrame!=frame){
   var values=new ArrayList<PhysicalWorld.Static>();var parent=MeshPlacement.place(fittedMesh,frame.cos(),frame.sin(),frame.scale());
   values.add(new PhysicalWorld.Static("storage-housing",parent,RigidPhysics.pose((float)frame.origin().x(),(float)frame.origin().y(),(float)frame.origin().height())));
   var p=IsoPlayer.players[0];var square=view.container.getParent().getSquare();
   for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++){var floor=p.getCell().getGridSquare(square.getX()+x,square.getY()+y,square.getZ());if(floor!=null&&floor.TreatAsSolidFloor())values.add(new PhysicalWorld.Static("floor:"+x+":"+y,CollisionShape.box(.5f,.5f,.01f,0,0,-.01f),RigidPhysics.pose(floor.getX()+.5f,floor.getY()+.5f,(float)(floor.getZ()*Math.sqrt(6)))));}
   bodies.environment(values);bodyFrame=frame;
  }
 }
 public static void physicsTick(){
  if(!physicsEnabled)return;var view=selected;long now=System.nanoTime();
  if(view==null||view.actor!=IsoPlayer.players[0]||!eligible(view.container)||zombie.network.GameClient.client||zombie.network.GameServer.server||ShelfBridge.supported(view.container.getParent())&&!ShelfBridge.ready(view.container.getParent())){closeBodies();return;}
  if(bodies==null)return;var keep=new ArrayList<InventoryItem>();for(var row:visible)if(row.item.getContainer()==view.container&&row.item.getWorldItem()==null)keep.add(row.item);
  if(residentPlan!=visible){saveBodies();residentPlan=visible;}bodies.retain(keep);
  if(!zombie.GameTime.isGamePaused()){
   bodies.tick(now);for(var iterator=released.entrySet().iterator();iterator.hasNext();){var e=iterator.next();if(bodies.sample(e.getKey())!=null){bodies.released(e.getKey(),e.getValue());iterator.remove();}}
  }
  if(now>=nextSave){saveBodies();nextSave=now+200_000_000L;}
 }
 public static void releaseStored(InventoryItem item,double vx,double vy,double vh){if(!physicsEnabled)return;if(released.size()>=64)released.clear();released.put(item,new float[]{(float)Math.clamp(vx,-12,12),(float)Math.clamp(vy,-12,12),(float)Math.clamp(vh,-12,12),0,0,0});}
 public static String status(){return status;}
 public static boolean physicsEnabled(){return physicsEnabled;}
 /** Renew only the already validated complete view, without reparsing inventory rows. */
 public static boolean refresh(ItemContainer container,String key){
  var view=selected;if(view==null||view.owner!=Thread.currentThread()||view.container!=container||!view.key.equals(key))return false;
  if(!eligible(container)||view.actor!=IsoPlayer.players[0]){retireCapture("Storage model capture inactive");return false;}
  selected=new Selection(container,key,view.rows,view.actor,view.owner,System.nanoTime());return true;
 }
 public static void publish(ItemContainer container,String key,KahluaTable data){
  if(!eligible(container)||key==null||key.isBlank()||key.length()>128||data==null){clear();return;}
  try{
   var items=container.getItems();if(items.size()>MAX_ITEMS)throw new IllegalArgumentException("Storage inventory budget");
   var ids=new HashMap<Integer,InventoryItem>();for(var item:items){if(item==null||item.getContainer()!=container||ids.put(item.getID(),item)!=null)throw new IllegalArgumentException("Storage inventory identity");}
   var rows=new ArrayList<Row>();var seen=new HashSet<Integer>();var values=data.iterator();int examined=0;
   while(values.advance()){
    if(++examined>MAX_ITEMS)throw new IllegalArgumentException("Storage metadata budget");
    int id=integer(values.getKey());if(!(values.getValue() instanceof KahluaTable row)||integer(row.rawget("id"))!=id||number(row,"v")!=1||!key.equals(row.rawget("key"))||!seen.add(id))throw new IllegalArgumentException("Storage metadata identity");
    var item=ids.get(id);if(item==null)continue;
    int slot=integer(row.rawget("slot"));double x=number(row,"x"),y=number(row,"y"),z=number(row,"z");
    float qx=(float)number(row,"qx"),qy=(float)number(row,"qy"),qz=(float)number(row,"qz"),qw=(float)number(row,"qw");double norm=qx*qx+qy*qy+qz*qz+qw*qw;
    boolean raw=Boolean.TRUE.equals(row.rawget("raw"));double low=raw?-.05:0,high=raw?1.05:1;
    if(slot<0||slot>=MAX_ITEMS||x<low||x>high||y<low||y>high||z<low||z>high||!Double.isFinite(norm)||Math.abs(norm-1)>.002)throw new IllegalArgumentException("Storage metadata pose");
    int revision=integer(row.rawget("rev"));if(revision<1||revision>2147483646)throw new IllegalArgumentException("Storage metadata revision");
    if(item.getWorldItem()==null)rows.add(new Row(item,slot,x,y,z,qx,qy,qz,qw,Boolean.TRUE.equals(row.rawget("auto")),Boolean.TRUE.equals(row.rawget("raw")),revision));
   }
   var previous=selected;if(previous!=null&&previous.container!=container)ShelfBridge.clear();List<Row> retained=previous!=null&&previous.container==container&&previous.key.equals(key)&&previous.rows.equals(rows)?previous.rows:rows;
   if(previous==null||retained!=previous.rows){contacts=List.of();packed.clear();}
   selected=new Selection(container,key,retained,IsoPlayer.players[0],Thread.currentThread(),System.nanoTime());status="Storage models armed: "+rows.size()+" native items";
  }catch(IllegalArgumentException invalid){clear();status="Storage model data rejected: "+invalid.getMessage();}
 }
 private static Access access()throws ReflectiveOperationException{
  var a=access;if(a!=null)return a;
  var own=Models.class.getDeclaredMethod("own",Frame.class,int.class,IsoGridSquare.class,IsoObject.class,InventoryItem.class);own.setAccessible(true);
  var refs=new ArrayList<Field>();for(String name:new String[]{"meshes","instances","textures","items","keys"}){var ref=field(ModelDraws.class,name);if(!ref.getType().isArray()||ref.getType().getComponentType().isPrimitive())throw new IllegalArgumentException("Storage packet reference schema");refs.add(ref);}
  a=new Access(field(Models.class,"capturing"),field(Models.class,"sink"),field(ModelDraws.class,"values"),field(ModelDraws.class,"meshes"),field(ModelDraws.class,"flags"),field(ModelDraws.class,"palette"),field(ModelDraws.class,"shadowPalette"),field(ModelDraws.class,"count"),List.copyOf(refs),own);access=a;return a;
 }
 private static Field field(Class<?> owner,String name)throws ReflectiveOperationException{var f=owner.getDeclaredField(name);f.setAccessible(true);return f;}
 private static StorageFrame frame(ItemContainer container){
  var owner=container.getParent();var square=owner.getSquare();if(owner.getSprite()==null||!ChunkCache.packsOn(square))return null;
  var actual=ShelfBridge.binding(owner);var binding=actual==null?ModelPacks.bind(owner.getSprite()):actual.binding();if(binding==null)return null;int state=binding.model.state();if(state!=PackModel.READ&&state!=PackModel.RESIDENT)return null;
  var mesh=PackedModelGeometry.get(binding.model);if(mesh==null){MeshRecovery.request(binding.model);return null;}
  boolean turns=actual==null?PackBind.turns(owner.getSprite()):actual.turns();float c=turns?binding.cos:1,s=turns?binding.sin:0;
  float x=square.getX()+.5f+binding.x,y=square.getY()+.5f+binding.y,h=(float)(square.getZ()*Math.sqrt(6))+WorldMesher.rise(owner)+binding.z;
  if(fittedOwner==owner&&fittedModel==binding.model&&fittedMesh==mesh&&fittedCos==c&&fittedSin==s&&fittedScale==binding.scale&&fittedX==x&&fittedY==y&&fittedH==h)return fitted;
  float[] v=mesh.vertices();float minX=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY,minD=minX,maxD=maxX,minH=minX,maxH=maxX;
  for(int n=0;n<v.length;n+=3){minX=Math.min(minX,v[n]);maxX=Math.max(maxX,v[n]);minD=Math.min(minD,v[n+2]);maxD=Math.max(maxD,v[n+2]);minH=Math.min(minH,v[n+1]);maxH=Math.max(maxH,v[n+1]);}
  packed.clear();fitted=new StorageFrame(new ContactFrame.Point(x,y,h),c,s,binding.scale,minX,maxX,minD,maxD,minH,maxH);
  try{occlusion=new StorageOcclusion(v,mesh.faces());}catch(IllegalArgumentException unsupported){occlusion=null;}
  fittedOwner=owner;fittedModel=binding.model;fittedMesh=mesh;fittedCos=c;fittedSin=s;fittedScale=binding.scale;fittedX=x;fittedY=y;fittedH=h;return fitted;
 }
 private static List<Row> nearest(Selection view,StorageFrame frame,Frame nativeFrame,long now){
  if(plannedRows!=view.rows||plannedFrame!=frame){plannedRows=view.rows;plannedFrame=frame;located=view.rows.stream().map(row->new Located(row,position(row,frame))).toList();nextPlan=0;}
  if(now<nextPlan)return visible;nextPlan=now+100_000_000L;
  var points=new ArrayList<ContactFrame.Point>();points.add(new ContactFrame.Point(nativeFrame.camX,nativeFrame.camY,nativeFrame.camZ*Math.sqrt(6)));
  var hands=PzvrContacts.sample(now);if(hands!=null)for(var hand:hands.hands())points.add(hand.centre());
  record Candidate(Row row,double distance){}
  var order=Comparator.comparingDouble(Candidate::distance).thenComparingInt(c->c.row.item.getID());var heap=new PriorityQueue<Candidate>(MAX_DRAWS,order.reversed());
  for(var entry:located){double distance=Double.POSITIVE_INFINITY;for(var point:points){double x=entry.point.x()-point.x(),y=entry.point.y()-point.y(),h=entry.point.height()-point.height();distance=Math.min(distance,x*x+y*y+h*h);}
   if(heap.size()<MAX_DRAWS)heap.add(new Candidate(entry.row,distance));else if(distance<heap.peek().distance||distance==heap.peek().distance&&entry.row.item.getID()<heap.peek().row.item.getID()){heap.poll();heap.add(new Candidate(entry.row,distance));}
  }
  visible=heap.stream().sorted(order).map(Candidate::row).toList();return visible;
 }
 private static StorageFrame.Bounds bounds(Frame frame,int first,int last,StorageFrame local,Access a)throws ReflectiveOperationException{
  if(last<=first||last-first>16)throw new IllegalArgumentException("Storage model part budget");
  var meshes=(ModelMesh[])a.meshes.get(frame.scene.models);var rows=(float[])a.values.get(frame.scene.models);var palettes=(int[])a.palette.get(frame.scene.models);var shadows=(int[])a.shadow.get(frame.scene.models);
  if(first<0||last>meshes.length||last>rows.length/STRIDE||last>palettes.length||last>shadows.length)throw new IllegalArgumentException("Storage packet extent");
  double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY,minD=minX,maxD=maxX,minH=minX,maxH=maxX;
  for(int n=first;n<last;n++){
   var mesh=meshes[n];if(mesh==null||mesh.skinningData!=null||palettes[n]>=0||shadows[n]>=0||!mesh.minXyz.isFinite()||!mesh.maxXyz.isFinite())throw new IllegalArgumentException("Storage rigid mesh bounds");
   var matrix=new Matrix4f().set(rows,n*STRIDE);if(!matrix.isFinite())throw new IllegalArgumentException("Storage model transform");
   for(int mask=0;mask<8;mask++){
    var p=matrix.transformPosition(new Vector3f((mask&1)==0?mesh.minXyz.x:mesh.maxXyz.x,(mask&2)==0?mesh.minXyz.y:mesh.maxXyz.y,(mask&4)==0?mesh.minXyz.z:mesh.maxXyz.z));
    var point=new ContactFrame.Point(frame.camX-p.x,frame.camY-p.z,frame.camZ*Math.sqrt(6)+p.y);var q=local==null?point:local.local(point);
    minX=Math.min(minX,q.x());maxX=Math.max(maxX,q.x());minD=Math.min(minD,q.y());maxD=Math.max(maxD,q.y());minH=Math.min(minH,q.height());maxH=Math.max(maxH,q.height());
   }
  }return new StorageFrame.Bounds(minX,maxX,minD,maxD,minH,maxH);
 }
 private static void discard(Frame frame,int first,Access a)throws IllegalAccessException{
  var draws=frame.scene.models;int last=draws.count();if(first<0||first>last)throw new IllegalArgumentException("Storage rollback range");
  var flags=(int[])a.flags.get(draws);if(last>flags.length)throw new IllegalArgumentException("Storage rollback flags");
  for(var ref:a.refs){Object array=ref.get(draws);if(last>Array.getLength(array))throw new IllegalArgumentException("Storage rollback references");}
  for(var ref:a.refs){Object array=ref.get(draws);for(int n=first;n<last;n++)Array.set(array,n,null);}
  Arrays.fill(flags,first,last,0);a.count.setInt(draws,first);
 }
 private static ContactFrame.Point fit(Frame frame,int first,int last,Row row,StorageFrame fitted,ContactFrame.Point point,Access a)throws ReflectiveOperationException{
  if(last<=first||last-first>16)throw new IllegalArgumentException("Storage part fitting budget");
  var meshArray=(ModelMesh[])a.meshes.get(frame.scene.models);var values=(float[])a.values.get(frame.scene.models);
  if(first<0||last>meshArray.length||last>values.length/STRIDE)throw new IllegalArgumentException("Storage fitting packet budget");
  var meshes=new ArrayList<ModelMesh>();long[] signature=new long[9+(last-first)*18];int next=0;
  signature[next++]=Double.doubleToLongBits(row.x);signature[next++]=Double.doubleToLongBits(row.y);signature[next++]=Double.doubleToLongBits(row.z);
  for(float q:new float[]{row.qx,row.qy,row.qz,row.qw})signature[next++]=Float.floatToIntBits(q);signature[next++]=row.automatic?1:0;signature[next++]=row.raw?1:0;
  for(int n=first;n<last;n++){
   var mesh=meshArray[n];if(mesh==null||mesh.skinningData!=null)throw new IllegalArgumentException("Storage fitting rigid mesh");meshes.add(mesh);
   for(float v:new float[]{mesh.minXyz.x,mesh.minXyz.y,mesh.minXyz.z,mesh.maxXyz.x,mesh.maxXyz.y,mesh.maxXyz.z})signature[next++]=Float.floatToIntBits(v);
   for(int m:new int[]{0,1,2,4,5,6,8,9,10})signature[next++]=Float.floatToIntBits(values[n*STRIDE+m]);
   signature[next++]=Math.round((values[n*STRIDE+12]-(frame.camX-point.x()))*500);
   signature[next++]=Math.round((values[n*STRIDE+13]-(point.height()-frame.camZ*Math.sqrt(6)))*500);
   signature[next++]=Math.round((values[n*STRIDE+14]-(frame.camY-point.y()))*500);
  }
  var cached=solved.get(row.item);if(cached!=null&&cached.frame.equals(fitted)&&cached.meshes.equals(meshes)&&Arrays.equals(cached.signature,signature))return cached.position;
  fittingQueries++;var volume=bounds(frame,first,last,fitted,a);var position=fitted.fit(point,volume);
  if(position!=null&&row.automatic){
   if(occlusion==null)throw new IllegalArgumentException("Original support geometry unavailable");
   var initial=fitted.local(point);var moved=fitted.local(position);double shiftedMin=volume.minH()+moved.height()-initial.height();
   double support=occlusion.support((volume.minX()+volume.maxX())/2+moved.x()-initial.x(),(volume.minD()+volume.maxD())/2+moved.y()-initial.y(),shiftedMin+.05/fitted.scale());
   if(!Double.isFinite(support)||support< fitted.minH()||support+volume.maxH()-volume.minH()+.002/fitted.scale()>fitted.maxH())position=null;
   else position=fitted.world(moved.x(),moved.y(),moved.height()+support+.002/fitted.scale()-shiftedMin);
  }
  if(solved.size()>=128&&!solved.containsKey(row.item))solved.clear();solved.put(row.item,new Solved(fitted,List.copyOf(meshes),signature,position));return position;
 }
 private static boolean automatic(Row row,Map<InventoryItem,StorageBodies.Sample> states){return row.automatic&&!row.raw&&!states.containsKey(row.item);}
 private static StoragePacking.Result pack(Row row,StorageFrame frame,ContactFrame.Point point,StorageFrame.Bounds bounds,List<StorageFrame.Bounds> occupied,StoragePacking.Budget budget){
  var old=packed.get(row.item);
  if(old!=null&&old.frame==frame&&old.bounds.equals(bounds)&&old.pivot.equals(point)&&old.occupied.equals(occupied))return new StoragePacking.Result(old.position,false,0);
  packingQueries++;var result=StoragePacking.place(frame,occlusion,point,bounds,occupied,budget);
  if(!result.exhausted()){if(packed.size()>=128&&!packed.containsKey(row.item))packed.clear();packed.put(row.item,new PackedSolution(frame,bounds,point,List.copyOf(occupied),result.position()));}return result;
 }
 public static void capture(Frame frame){
  var view=selected;long now=System.nanoTime();if(view==null){contacts=List.of();return;}if(view.owner!=Thread.currentThread())return;
  if(frame==null||view.actor!=IsoPlayer.players[0]||now<view.time||now-view.time>1_500_000_000L||!eligible(view.container)){retireCapture("Storage model capture inactive");return;}
  int drawn=0,missing=0,oversized=0,records=0,automaticPlaced=0,noRoom=0,deferred=0;var live=new ArrayList<Contact>();int firstCapture=frame.scene.models.count();
  try{
   var fitted=frame(view.container);if(fitted==null){retireCapture("Storage models waiting for original furniture geometry");return;}
   prepareBodies(view,fitted);missingPhysics=0;
   var a=access();var square=view.container.getParent().getSquare();
   var planned=new ArrayList<>(nearest(view,fitted,frame,now));var states=new IdentityHashMap<InventoryItem,StorageBodies.Sample>();
   if(bodies!=null)for(var row:planned){var state=bodies.sample(row.item);if(state!=null&&state.revision()>=row.revision)states.put(row.item,state);}
   planned.sort(Comparator.comparing((Row row)->automatic(row,states)).thenComparingInt(Row::slot).thenComparingInt(row->row.item.getID()));
   var occupied=new ArrayList<StorageFrame.Bounds>();var packingBudget=new StoragePacking.Budget(StoragePacking.MAX_GEOMETRY_QUERIES);boolean fixedUnavailable=false;
   var plannedItems=Collections.newSetFromMap(new IdentityHashMap<InventoryItem,Boolean>());for(var row:planned)plannedItems.add(row.item);
   for(var row:view.rows)if(!plannedItems.contains(row.item)){var state=bodies==null?null:bodies.sample(row.item);if(!row.automatic||row.raw||state!=null&&state.revision()>=row.revision){fixedUnavailable=true;break;}}
   boolean readyHousing=!ShelfBridge.supported(view.container.getParent())||ShelfBridge.ready(view.container.getParent());
   try(var lease=new StorageCaptureLease(a.flag,a.sink,frame.drawers)){
    for(var row:planned){
     boolean automatic=automatic(row,states);
     var item=row.item;if(item.getContainer()!=view.container||item.getWorldItem()!=null||!view.container.contains(item))continue;
     if(automatic&&readyHousing&&fixedUnavailable){deferred++;continue;}
     Long after=retry.get(item);if(after!=null&&now<after){missing++;if(!automatic)fixedUnavailable=true;continue;}
     if(records>MAX_RECORDS-16)break;
     int first=frame.scene.models.count(),start=frame.drawers.size();
     try{
     var point=position(row,fitted);
      if(!ItemModelRenderer.itemHasModel(item)||WorldItemModelDrawer.renderMain(item,square,square,(float)point.x(),(float)point.y(),(float)(point.height()/Math.sqrt(6)),0,0,false)!=ItemModelRenderer.RenderStatus.Ready){missing++;if(!automatic)fixedUnavailable=true;retry.put(item,now+1_000_000_000L);continue;}
      a.own.invoke(null,frame,start,square,null,item);int last=frame.scene.models.count();if(last-first+records>MAX_RECORDS)throw new IllegalArgumentException("Storage scene record budget");
      var q=fitted.rotation(row.qx,row.qy,row.qz,row.qw);var pose=new float[]{(float)point.x(),(float)point.y(),(float)point.height(),q[0],q[1],q[2],q[3]};
      DroppedItemPose.apply(frame,first,last,point,pose);var position=row.raw&&!row.automatic?point:fit(frame,first,last,row,fitted,point,a);
      if(position==null){discard(frame,first,a);oversized++;if(!automatic)fixedUnavailable=true;continue;}
      DroppedItemPose.apply(frame,first,last,point,new float[]{(float)position.x(),(float)position.y(),(float)position.height(),0,0,0,1});
      if(automatic&&readyHousing){
       var result=fixedUnavailable?new StoragePacking.Result(null,true,0):pack(row,fitted,position,bounds(frame,first,last,fitted,a),occupied,packingBudget);
       if(result.position()==null){discard(frame,first,a);if(result.exhausted())deferred++;else noRoom++;if(bodies!=null&&bodies.sample(item)==null)bodies.remove(item);continue;}
       var placed=result.position();DroppedItemPose.apply(frame,first,last,position,new float[]{(float)placed.x(),(float)placed.y(),(float)placed.height(),0,0,0,1});
       var rendered=bounds(frame,first,last,fitted,a);double epsilon=.0001/fitted.scale();
       if(!StoragePacking.free(rendered,occupied,0)||rendered.minX()<fitted.minX()-epsilon||rendered.maxX()>fitted.maxX()+epsilon||rendered.minD()<fitted.minD()-epsilon||rendered.maxD()>fitted.maxD()+epsilon||rendered.minH()<fitted.minH()-epsilon||rendered.maxH()>fitted.maxH()+epsilon){discard(frame,first,a);noRoom++;if(bodies!=null&&bodies.sample(item)==null)bodies.remove(item);continue;}
       position=placed;automaticPlaced++;
      }
      if(bodies!=null){
       var bodyPose=new float[]{(float)position.x(),(float)position.y(),(float)position.height(),q[0],q[1],q[2],q[3]};
       Long afterGeometry=geometryRetry.get(item);
       if(afterGeometry==null||now>=afterGeometry)try{var shape=StorageItemGeometry.read(frame,first,last,bodyPose,item);bodies.offer(item,shape,bodyPose,Math.clamp(item.getActualWeight(),.01f,50),row.revision,!row.automatic&&!released.containsKey(item));geometryRetry.remove(item);}
       catch(IllegalArgumentException unsupported){missingPhysics++;geometryRetry.put(item,now+1_000_000_000L);}
       else missingPhysics++;
       var state=bodies.sample(item);
       if(state!=null&&state.revision()>=row.revision){var p=state.state();var delta=new Quaternionf(p[3],p[4],p[5],p[6]).mul(new Quaternionf(q[0],q[1],q[2],q[3]).invert()).normalize();DroppedItemPose.apply(frame,first,last,position,new float[]{p[0],p[1],p[2],delta.x,delta.y,delta.z,delta.w});}
      }
      var contact=bounds(frame,first,last,null,a);occupied.add(bounds(frame,first,last,fitted,a));live.add(new Contact(item,view.container,square,contact,now));drawn++;retry.remove(item);
     }catch(ReflectiveOperationException|RuntimeException unsupported){discard(frame,first,a);missing++;if(!automatic)fixedUnavailable=true;retry.put(item,now+1_000_000_000L);}
     finally{records+=Math.max(0,frame.scene.models.count()-first);}
    }
   }
   var owner=view.container.getParent();boolean shelf=ShelfBridge.supported(owner);
   if(shelf){ShelfBridge.request(owner,drawn>0);if(drawn>0&&!ShelfBridge.ready(owner)){discard(frame,firstCapture,a);closeBodies();contacts=List.of();status="Storage assets ready; waiting for native empty shelf housing";return;}}else ShelfBridge.request(owner,false);
   if(selected==view&&eligible(view.container)){contacts=List.copyOf(live);status="Storage: "+drawn+"/"+view.rows.size()+" native models nearby, "+missing+" unavailable, "+oversized+" do not fit; auto placed "+automaticPlaced+", no room "+noRoom+", deferred "+deferred+"; fitting queries "+fittingQueries+", packing queries "+packingQueries+(bodies==null?"":"; bodies "+bodies.count()+", pending "+bodies.pending()+", missing geometry "+missingPhysics);}else contacts=List.of();
   if(retry.size()>MAX_ITEMS)retry.clear();
  }catch(ReflectiveOperationException|RuntimeException|LinkageError error){retireCapture("Storage model capture unavailable");if(now-warning>5_000_000_000L){warning=now;System.err.println("[PZVR Storage] Native capture retained: "+error);}}
 }
 public static Contact nearest(ContactFrame.Point hand,long now){
  if(fitted==null||occlusion==null||ShelfBridge.supported(fittedOwner)&&!ShelfBridge.ready(fittedOwner))return null;
  var candidates=new ArrayList<Contact>();
  for(var contact:contacts){if(now<contact.time||now-contact.time>=150_000_000L||contact.container.getParent()!=fittedOwner||!eligible(contact.container)||contact.item.getContainer()!=contact.container||contact.item.getWorldItem()!=null||!contact.container.contains(contact.item)||contact.bounds.distance(hand)>=.12)continue;candidates.add(contact);}
  candidates.sort(Comparator.comparingDouble(c->c.bounds.distance(hand)));
  for(int n=0;n<Math.min(4,candidates.size());n++){
   var contact=candidates.get(n);var b=contact.bounds;var centre=new ContactFrame.Point((b.minX()+b.maxX())/2,(b.minD()+b.maxD())/2,(b.minH()+b.maxH())/2);
   var target=new ContactFrame.Point(Math.clamp(hand.x(),b.minX(),b.maxX()),Math.clamp(hand.y(),b.minD(),b.maxD()),Math.clamp(hand.height(),b.minH(),b.maxH()));
   if(occlusion.aperture(fitted,fitted.local(centre))&&!occlusion.blocked(fitted.local(hand),fitted.local(target)))return contact;
  }return null;
 }
 public static Destination destination(WorldRigFrame.Hand hand,InventoryItem item,long now){
  var view=selected;if(view==null||hand==null||item==null||fitted==null||occlusion==null||fittedOwner!=view.container.getParent()||now<view.time||now-view.time>1_500_000_000L||!eligible(view.container)||ShelfBridge.supported(fittedOwner)&&!ShelfBridge.ready(fittedOwner))return null;
  float[] pose=StoragePose.hand(hand),local=StoragePose.normalized(fitted,pose);if(local==null||!occlusion.aperture(fitted,fitted.local(hand.grip())))return null;
  for(int n=0;n<3;n++)if(local[n]<0||local[n]>1)return null;
  var player=IsoPlayer.players[0];if(!view.container.hasRoomFor(player,item))return null;
  return new Destination(view.container,view.container.getParent().getSquare(),view.key,local);
 }
 private static ContactFrame.Point position(Row row,StorageFrame frame){return row.raw?frame.rawPosition(row.x,row.y,row.z):frame.position(row.x,row.y,row.z);}
 private NativeStorage(){}
}
