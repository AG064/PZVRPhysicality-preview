package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Exposer.LuaClass;
import zombie.characters.IsoPlayer;import zombie.inventory.InventoryItem;import zombie.iso.IsoGridSquare;import zombie.iso.objects.IsoWorldInventoryObject;
@LuaClass(name="PZVRPhysicality")
public final class Main {
 private static volatile boolean ready;private static String failure="SDK validation pending";
 private static final java.util.concurrent.atomic.AtomicLong configurationErrors=new java.util.concurrent.atomic.AtomicLong();
 public static long configurationFailures(){return configurationErrors.get();}
 public static boolean presetValid(boolean items,boolean consumption,boolean taps,boolean fingers,boolean storagePhysics,boolean body,boolean curtains,boolean crafting){return ready&&NativeItems.enabled()==items&&NativeConsumption.enabled()==consumption&&NativeTaps.enabled()==taps&&FingerPoseDriver.enabled()==fingers&&NativeStorage.physicsEnabled()==storagePhysics&&NativeBody.enabled()==body&&RouteModes.body()==body&&NativeCurtains.enabled()==curtains&&RouteModes.curtains()==curtains&&NativeCrafting.enabled()==crafting&&RouteModes.crafting()==crafting;}
 public static void main(String[] args){try{SdkPins.verifyRuntime();ready=true;System.out.println("[PZVR Physicality] SDK owners verified; extension is Off by default");}catch(Exception|LinkageError error){failure=error.toString();System.err.println("[PZVR Physicality] Unavailable: "+failure);}}
 public static boolean ready(){return ready;}
 public static String sdkStatus(){return ready?"SDK owners verified":"Unavailable: "+failure;}
 public static String storageProvidersConfigure(boolean enabled){try{if(enabled&&!ready)return "Unavailable: "+failure;StorageProviders.configure(enabled);return storageProvidersStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Storage providers unavailable: "+error;}}
 public static boolean storageProvidersEnabled(){try{return ready&&StorageProviders.enabled();}catch(RuntimeException|LinkageError unavailable){return false;}}
 public static String storageProvidersStatus(){try{return ready?StorageProviders.status():"Unavailable: "+failure;}catch(RuntimeException|LinkageError unavailable){return "Storage providers unavailable";}}
 public static String packCurtainsConfigure(boolean enabled){try{if(enabled&&!ready)return "Unavailable: "+failure;PackCurtainCapture.configure(enabled);return packCurtainsStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Pack curtain capture unavailable: "+error;}}
 public static boolean packCurtainsEnabled(){try{return ready&&PackCurtainCapture.enabled();}catch(RuntimeException|LinkageError unavailable){return false;}}
 public static String packCurtainsStatus(){try{return ready?PackCurtainCapture.status():"Unavailable: "+failure;}catch(RuntimeException|LinkageError unavailable){return "Pack curtain capture unavailable";}}
 public static boolean guideShowHud(){return GuideUiRoute.show(ready);}public static boolean guideReturnToGame(){return GuideUiRoute.returnToGame(ready);}public static String guideUiStatus(){return GuideUiRoute.status(ready)+"; "+GuideUiRoute.controls(ready);}
 public static String interactionStatus(){return NativeItems.guardStatus()+"; "+PzvrContacts.status()+"; "+WorldRigCapture.status();}
 public static String inputStatus(){return ControllerTouch.status();}
 public static String nativeHeldConfigure(boolean enabled){try{if(enabled&&!ready)return "Unavailable: "+failure;NativeHeldFallback.configure(enabled);return nativeHeldStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Native held model fallback unavailable: "+error;}}
 public static boolean nativeHeldEnabled(){return NativeHeldFallback.enabled();}public static String nativeHeldStatus(){return NativeHeldFallback.status();}public static boolean presetNativeHeldValid(boolean enabled){return ready&&NativeHeldFallback.enabled()==enabled;}
 public static String windowMotionConfigure(boolean enabled){try{if(enabled&&!ready)return "Unavailable: "+failure;WindowMotion.configure(enabled);return windowMotionStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Window sash motion unavailable: "+error;}}
 public static boolean windowMotionEnabled(){return WindowMotion.enabled();}public static String windowMotionStatus(){return WindowMotion.status();}public static boolean presetWindowMotionValid(boolean enabled){return ready&&WindowMotion.enabled()==enabled;}
 public static void windowMotionTick(){if(!ready)return;try{WindowMotion.tick();}catch(RuntimeException|LinkageError error){try{WindowMotion.configure(false);}catch(RuntimeException|LinkageError cleanup){error.addSuppressed(cleanup);}System.err.println("[PZVR Window motion] Disabled: "+error);}}
 public static String modesStatus(){return "Items="+NativeItems.enabled()+", consumption="+NativeConsumption.enabled()+", taps="+NativeTaps.enabled()+", fingers="+FingerPoseDriver.enabled()+", storage physics="+NativeStorage.physicsEnabled()+", body="+NativeBody.enabled()+", curtains="+NativeCurtains.enabled()+", crafting="+NativeCrafting.enabled()+", backpack="+BackpackContents.enabled()+", held contacts="+HeldContactDriver.enabled()+", indexed models="+ItemModelFallback.enabled()+", player collision="+NativePlayerCollision.enabled();}
 public static String indexedModelsConfigure(boolean enabled){try{if(enabled&&!ready)return "Unavailable: "+failure;ItemModelFallback.configure(enabled);return indexedModelsStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Indexed models unavailable: "+error;}}
 public static boolean indexedModelsEnabled(){return ItemModelFallback.enabled();}
 public static String indexedModelsStatus(){return ItemModelFallback.status();}
 public static boolean presetPresentationValid(boolean indexedModels){return ready&&ItemModelFallback.enabled()==indexedModels;}
 public static String playerCollisionConfigure(boolean enabled,boolean roomScaleBody,double radiusCm){try{if(enabled&&!ready)return "Unavailable: "+failure;NativePlayerCollision.configure(enabled,roomScaleBody,radiusCm);return playerCollisionStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Player collision unavailable: "+error;}}
 public static String playerCollisionStatus(){return NativePlayerCollision.status();}
 public static boolean presetPlayerCollisionValid(boolean enabled,boolean roomScaleBody){return ready&&NativePlayerCollision.enabled()==enabled&&NativePlayerCollision.roomScaleEnabled()==roomScaleBody;}
 public static String playerCollisionHint(){return NativePlayerCollision.roomContactHint();}
 public static double playerCollisionHintAlpha(){return NativePlayerCollision.roomContactAlpha();}
 public static String heldModelDiagnosticsConfigure(boolean enabled){try{if(enabled&&!ready)return "Unavailable: "+failure;ItemModelDiagnostics.configure(enabled);return heldModelDiagnosticsStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Held model diagnostics unavailable: "+error;}}
 public static String heldModelDiagnosticsStatus(){return ItemModelDiagnostics.status();}
 public static String handStateStatus(){return NativeHandState.status(ready&&NativeItems.gestureBusy(0),ready&&NativeItems.gestureBusy(1));}
 public static String gripAttemptsConfigure(boolean enabled){try{if(enabled&&!ready)return "Unavailable: "+failure;GripAttemptDiagnostics.configure(enabled);return gripAttemptsStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Grip attempt receipts unavailable: "+error;}}
 public static String gripAttemptsStatus(){return GripAttemptDiagnostics.status();}
 public static void gripAttemptsReset(){GripAttemptDiagnostics.reset();}
 public static String moveableModelsConfigure(boolean enabled){try{if(enabled&&!ready)return "Unavailable: "+failure;MoveableModels.configure(enabled);return moveableModelsStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Moveable ground models unavailable: "+error;}}
 public static boolean moveableModelsEnabled(){return MoveableModels.enabled();}
 public static String moveableModelsStatus(){return MoveableModels.status();}
 public static void moveableModelsTick(){if(!ready)return;try{MoveableModels.tick();}catch(RuntimeException|LinkageError error){try{MoveableModels.configure(false);}catch(RuntimeException|LinkageError cleanup){error.addSuppressed(cleanup);}System.err.println("[PZVR Moveable models] Disabled: "+error);}}
 public static boolean presetMoveableModelsValid(boolean enabled){return ready&&MoveableModels.enabled()==enabled;}
 public static String worldVisualsConfigure(boolean enabled){try{if(enabled&&!ready)return "Unavailable: "+failure;WorldVisuals.configure(enabled);return worldVisualsStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "World visuals unavailable: "+error;}}
 public static String worldVisualsStatus(){return WorldVisuals.status();}
 public static boolean presetWorldVisualsValid(boolean enabled){return ready&&WorldVisuals.enabled()==enabled;}
 public static void worldVisualsTick(){if(!ready)return;try{WorldVisuals.tick();}catch(RuntimeException|LinkageError error){try{WorldVisuals.configure(false);}catch(RuntimeException|LinkageError cleanup){error.addSuppressed(cleanup);}System.err.println("[PZVR World visuals] Disabled: "+error);}}
 private static void reconcileItems(){try{WorldContactCapture.requestItems(NativeItems.enabled());RouteModes.world(WorldContactCapture.enabled());}catch(RuntimeException|LinkageError unavailable){RouteModes.world(false);System.err.println("[PZVR Physicality] Item mode reconciliation unavailable: "+unavailable);}}
 private static void reconcileStorage(){try{WorldContactCapture.requestStorage(NativeStorage.physicsEnabled());RouteModes.world(WorldContactCapture.enabled());}catch(RuntimeException|LinkageError unavailable){RouteModes.world(false);System.err.println("[PZVR Storage] Mode reconciliation unavailable: "+unavailable);}}
 public static String enable(boolean value){try{if(value&&!ready)return "Unavailable: "+failure;NativeItems.configure(value);return NativeItems.status();}catch(Exception|LinkageError error){configurationErrors.incrementAndGet();return "Unavailable: "+error;}finally{reconcileItems();}}
 public static String status(){return NativeItems.status();}
 public static void storagePublish(zombie.inventory.ItemContainer container,String key,se.krka.kahlua.vm.KahluaTable rows){if(ready)NativeStorage.publish(container,key,rows);}
 public static boolean storageRefresh(zombie.inventory.ItemContainer container,String key){return ready&&NativeStorage.refresh(container,key);}
 public static void storageClear(){NativeStorage.clear();}public static String storageStatus(){return NativeStorage.status();}
 public static String storagePhysicsConfigure(boolean enabled){try{if(enabled&&!ready)return "SDK validation pending";NativeStorage.physicsConfigure(enabled);return NativeStorage.status();}catch(Exception|LinkageError error){configurationErrors.incrementAndGet();return "Storage physics unavailable: "+error;}finally{reconcileStorage();}}
 public static void storagePhysicsTick(){try{NativeStorage.physicsTick();}catch(RuntimeException|LinkageError error){try{NativeStorage.physicsConfigure(false);}catch(Exception|LinkageError cleanup){error.addSuppressed(cleanup);}System.err.println("[PZVR Storage] Physics disabled: "+error);}finally{reconcileStorage();}}
 public static void tick(){moveableModelsTick();worldVisualsTick();windowMotionTick();GripAttemptDiagnostics.begin(ready);try{NativeItems.tick();}catch(RuntimeException|LinkageError error){try{NativeItems.configure(false);}catch(Exception|LinkageError cleanup){error.addSuppressed(cleanup);}System.err.println("[PZVR Physicality] Disabled after error: "+error);}finally{reconcileItems();GripAttemptDiagnostics.end();}}
 private static NativeItems.Request r(){return NativeItems.request();}
 public static boolean actionAllowed(IsoPlayer actor,int side,long epoch){if(!ready)return false;try{return NativeItems.actionAllowed(actor,side,epoch);}catch(RuntimeException|LinkageError unavailable){return false;}}
 public static long actionTicket(){if(!ready)return 0;try{return NativeItems.requestTicket();}catch(RuntimeException|LinkageError unavailable){return 0;}}
 public static boolean actionAllowedRequest(IsoPlayer actor,int side,long epoch,long ticket){if(!ready)return false;try{return NativeItems.actionAllowedRequest(actor,side,epoch,ticket);}catch(RuntimeException|LinkageError unavailable){return false;}}
 public static int action(){return r()==null?0:r().kind();}public static IsoPlayer player(){return IsoPlayer.players[0];}
 public static InventoryItem item(){return r()==null?null:r().item();}public static IsoWorldInventoryObject source(){return r()==null?null:r().source();}public static IsoGridSquare square(){return r()==null?null:r().square();}
 public static zombie.inventory.ItemContainer inventorySource(){return r()==null?null:r().container();}
 public static float qx(){return r()==null?0:r().rotation()[0];}public static float qy(){return r()==null?0:r().rotation()[1];}public static float qz(){return r()==null?0:r().rotation()[2];}public static float qw(){return r()==null?1:r().rotation()[3];}
 private static float[] storagePose(){return r()==null||r().storage()==null?new float[]{0,0,0,0,0,0,1}:r().storage().normalized();}
 public static String storageKey(){return r()==null||r().storage()==null?null:r().storage().key();}
 public static double storageX(){return storagePose()[0];}public static double storageY(){return storagePose()[1];}public static double storageZ(){return storagePose()[2];}
 public static double storageQx(){return storagePose()[3];}public static double storageQy(){return storagePose()[4];}public static double storageQz(){return storagePose()[5];}public static double storageQw(){return storagePose()[6];}
 public static int side(){return r()==null?-1:r().side();}public static long generation(){return NativeItems.epoch();}
 public static boolean primaryHand(int side){return side==1;}
 public static double x(){return r()==null?0:r().point().x()-r().square().getX();}public static double y(){return r()==null?0:r().point().y()-r().square().getY();}public static double z(){return r()==null?0:r().point().height()/Math.sqrt(6)-r().square().getZ();}
 public static double vx(){return r()==null?0:r().velocity()[0];}public static double vy(){return r()==null?0:r().velocity()[1];}public static double vh(){return r()==null?0:r().velocity()[2];}
 public static void acknowledge(boolean value){NativeItems.acknowledge(value);}
 public static boolean canReceive(IsoPlayer p,InventoryItem i,int side,long generation){return NativeItems.canReceive(p,i,side,generation);}public static void received(IsoPlayer p,InventoryItem i,int side,long generation){NativeItems.received(p,i,side,generation);}
 public static void dropped(IsoPlayer p,InventoryItem i,int side,long generation,double vx,double vy,double vh){NativeItems.dropped(p,i,side,generation,vx,vy,vh);}
 public static void droppedOriented(IsoPlayer p,InventoryItem i,int side,long generation,double vx,double vy,double vh,double qx,double qy,double qz,double qw){NativeItems.dropped(p,i,side,generation,vx,vy,vh,(float)qx,(float)qy,(float)qz,(float)qw);}
 public static void stored(IsoPlayer p,InventoryItem i,int side,long generation,zombie.inventory.ItemContainer destination){NativeItems.stored(p,i,side,generation,destination);}
 public static void storedReleased(IsoPlayer p,InventoryItem i,int side,long generation,zombie.inventory.ItemContainer destination,double vx,double vy,double vh){if(NativeItems.stored(p,i,side,generation,destination))NativeStorage.releaseStored(i,vx,vy,vh);}
 public static String consumptionConfigure(boolean enabled,boolean trigger,double downCm,double reachCm){try{if(enabled&&!ready)return "Unavailable: "+failure;NativeConsumption.configure(enabled,trigger,downCm/100,reachCm/100);BiteRenderHook.reset();return consumptionStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Unavailable: "+error;}}
 public static void consumptionTick(){try{NativeConsumption.tick();}catch(RuntimeException|LinkageError error){NativeConsumption.configure(false,true,.11,.105);System.err.println("[PZVR Physicality] Consumption disabled: "+error);}}
 private static ConsumePolicy.Request<InventoryItem> c(){return NativeConsumption.request();}
 public static int consumptionAction(){return c()==null?0:c().kind()==ConsumePolicy.Kind.FOOD?1:c().kind()==ConsumePolicy.Kind.UTENSIL?3:2;}
 public static InventoryItem consumptionTool(){return NativeConsumption.tool();}
 public static boolean consumptionPrimaryHand(int side){return side==1;}
 public static String fingersConfigure(boolean enabled){try{if(enabled&&!ready)return "Unavailable: "+failure;FingerPoseDriver.configure(enabled);return fingersStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Finger poses unavailable: "+error;}}
 public static String fingersStatus(){return FingerPoseDriver.status()+"; "+ControllerTouch.status();}
 public static boolean curtainsAvailable(){return ready&&NativeCurtains.available();}
 public static boolean presetExtrasValid(boolean backpack,boolean contacts){return ready&&BackpackContents.enabled()==backpack&&RouteModes.backpack()==backpack&&HeldContactDriver.enabled()==contacts&&RouteModes.heldContacts()==contacts;}
 public static String backpackConfigure(boolean enabled,double reachCm){try{if(enabled&&!ready)return "Unavailable: "+failure;BackpackContents.configure(enabled,reachCm);RouteModes.backpack(BackpackContents.enabled());return backpackStatus();}catch(RuntimeException|LinkageError error){RouteModes.backpack(false);try{BackpackContents.configure(false,7.5);}catch(RuntimeException|LinkageError cleanup){error.addSuppressed(cleanup);}configurationErrors.incrementAndGet();return "Backpack unavailable: "+error;}}
 public static void backpackTick(){try{BackpackContents.tick();}catch(RuntimeException|LinkageError error){RouteModes.backpack(false);try{BackpackContents.configure(false,7.5);}catch(RuntimeException|LinkageError cleanup){error.addSuppressed(cleanup);}System.err.println("[PZVR Backpack] Disabled: "+error);}}
 private static BackpackContents.Request k(){return BackpackContents.request();}
 public static int backpackAction(){return k()==null?0:k().kind();}public static IsoPlayer backpackPlayer(){return k()==null?null:k().actor();}
 public static zombie.inventory.types.InventoryContainer backpackBag(){return k()==null?null:k().bag();}public static InventoryItem backpackItem(){return k()==null?null:k().item();}
 public static int backpackSide(){return k()==null?-1:k().side();}public static long backpackTicket(){return k()==null?0:k().ticket();}public static long backpackGeneration(){return k()==null?0:k().epoch();}
 public static boolean backpackValid(IsoPlayer p,zombie.inventory.types.InventoryContainer bag,InventoryItem item,int side,long ticket,long epoch,int phase){try{return BackpackContents.valid(p,bag,item,side,ticket,epoch,phase);}catch(RuntimeException|LinkageError invalid){return false;}}
 public static boolean backpackAcknowledge(long ticket,boolean queued){return BackpackContents.acknowledge(ticket,queued);}public static void backpackFinished(long ticket,boolean received){BackpackContents.finished(ticket,received);}
 public static void backpackPage(int direction){BackpackContents.page(direction);}public static String backpackStatus(){return BackpackContents.status();}
 private static void reconcileContacts(){try{boolean actual=HeldContactDriver.enabled();RouteModes.heldContacts(actual);WorldContactCapture.requestHeldContacts(actual);RouteModes.world(WorldContactCapture.enabled());}catch(RuntimeException|LinkageError unavailable){RouteModes.heldContacts(false);RouteModes.world(false);System.err.println("[PZVR Contacts] Mode reconciliation unavailable: "+unavailable);}}
 public static String contactsConfigure(boolean enabled,double frequency){try{if(enabled&&!ready)return "Unavailable: "+failure;HeldContactDriver.configure(enabled,frequency);return contactsStatus();}catch(Exception|LinkageError error){configurationErrors.incrementAndGet();return "Held contacts unavailable: "+error;}finally{reconcileContacts();}}
 public static void contactsTick(){try{HeldContactDriver.tick();}catch(RuntimeException|LinkageError error){try{HeldContactDriver.configure(false,1);}catch(Exception|LinkageError cleanup){error.addSuppressed(cleanup);}System.err.println("[PZVR Contacts] Disabled: "+error);}finally{reconcileContacts();}}
 public static String contactsStatus(){return HeldContactDriver.status();}public static String worldContactsStatus(){return WorldContactCapture.status();}
 public static String contactsVisualConfigure(boolean enabled,double maximumCm){try{HeldContactDriver.visualConstraints(enabled,maximumCm/100);return contactsStatus();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Contact visual configuration unavailable: "+error;}}
 public static String bodyConfigure(boolean enabled,double radiusCm){try{if(enabled&&!ready)return "Unavailable: "+failure;NativeBody.configure(enabled,radiusCm);RouteModes.body(NativeBody.enabled());return NativeBody.status();}catch(RuntimeException|LinkageError error){RouteModes.body(false);configurationErrors.incrementAndGet();return "Body storage unavailable: "+error;}}
 public static void bodyAnchor(int zone,double x,double y,double z){NativeBody.anchor(zone,x,y,z);}
 public static double bodyCalibration(int side,int axis){return NativeBody.calibration(side,axis);}
 public static void bodyBegin(IsoPlayer p){if(ready)NativeBody.begin(p);}
 public static void bodySlot(String id,int zone,InventoryItem item,String location,boolean left,boolean right){if(ready)NativeBody.slot(id,zone,item,location,left,right);}
 public static void bodyTick(){try{NativeBody.tick();}catch(RuntimeException|LinkageError error){RouteModes.body(false);NativeBody.configure(false,16);System.err.println("[PZVR Body] Disabled: "+error);}}
 private static NativeBody.Request b(){return NativeBody.request();}
 public static int bodyAction(){return b()==null?0:b().kind();}public static IsoPlayer bodyPlayer(){return b()==null?null:b().actor();}
 public static InventoryItem bodyItem(){return b()==null?null:b().item();}public static int bodySide(){return b()==null?-1:b().side();}public static String bodySlotId(){return b()==null?"":b().slot();}
 public static long bodyTicket(){return b()==null?0:b().ticket();}public static long bodyGeneration(){return b()==null?0:b().epoch();}
 public static boolean bodyValid(IsoPlayer p,InventoryItem item,int side,long ticket,long epoch){return NativeBody.valid(p,item,side,ticket,epoch);}
 public static boolean bodyAcknowledge(long ticket,boolean queued){return NativeBody.acknowledge(ticket,queued);}
 public static void bodyFinished(long ticket,boolean received){NativeBody.finished(ticket,received);}public static String bodyStatus(){return NativeBody.status();}
 public static String craftingConfigure(boolean enabled,double reachCm){try{if(enabled&&!ready)return "Unavailable: "+failure;NativeCrafting.configure(enabled,reachCm/100);RouteModes.crafting(NativeCrafting.enabled());return NativeCrafting.status();}catch(RuntimeException|LinkageError error){RouteModes.crafting(false);configurationErrors.incrementAndGet();return "Crafting unavailable: "+error;}}
 public static void craftingTick(){try{NativeCrafting.tick();}catch(RuntimeException|LinkageError error){RouteModes.crafting(false);NativeCrafting.configure(false,.14);System.err.println("[PZVR Crafting] Disabled: "+error);}}
 private static NativeCrafting.Request f(){return NativeCrafting.request();}
 public static IsoPlayer craftingPlayer(){return f()==null?null:f().actor();}public static InventoryItem craftingTool(){return f()==null?null:f().tool();}public static int craftingSide(){return f()==null?-1:f().side();}
 public static long craftingTicket(){return f()==null?0:f().ticket();}public static long craftingGeneration(){return f()==null?0:f().generation();}
 public static boolean craftingValid(IsoPlayer p,InventoryItem item,int side,long ticket,long epoch){return NativeCrafting.valid(p,item,side,ticket,epoch);}
 public static boolean craftingAcknowledge(long ticket,boolean opened){return NativeCrafting.acknowledge(ticket,opened);}public static void craftingShowHud(){NativeCrafting.showHud();}public static String craftingStatus(){return NativeCrafting.status();}
 public static String curtainsConfigure(boolean enabled){try{if(enabled&&!ready)return "Unavailable: "+failure;NativeCurtains.configure(enabled);RouteModes.curtains(NativeCurtains.enabled());return NativeCurtains.status();}catch(Exception|LinkageError error){RouteModes.curtains(false);configurationErrors.incrementAndGet();return "Curtains unavailable: "+error;}}
 public static void curtainsTick(){try{NativeCurtains.tick();}catch(RuntimeException|LinkageError error){RouteModes.curtains(false);try{NativeCurtains.configure(false);}catch(Exception|LinkageError cleanup){error.addSuppressed(cleanup);}System.err.println("[PZVR Curtains] Disabled: "+error);}}
 private static NativeCurtains.Request u(){return NativeCurtains.request();}
 public static IsoPlayer curtainsPlayer(){return u()==null?null:u().actor();}public static zombie.iso.objects.IsoCurtain curtainsSource(){return u()==null?null:u().source();}public static int curtainsSide(){return u()==null?-1:u().side();}
 public static long curtainsTicket(){return u()==null?0:u().ticket();}public static long curtainsGeneration(){return u()==null?0:u().epoch();}
 public static boolean curtainsValid(long ticket,long epoch){return NativeCurtains.valid(ticket,epoch);}public static boolean curtainsAcknowledge(long ticket,boolean queued){return NativeCurtains.acknowledge(ticket,queued);}
 public static void curtainsFinished(long ticket,boolean successful){NativeCurtains.finish(ticket,successful);}public static String curtainsStatus(){return NativeCurtains.status();}
 public static InventoryItem consumptionItem(){return c()==null?null:c().item();}public static int consumptionSide(){return c()==null?-1:c().side();}
 public static long consumptionTicket(){return c()==null?0:c().ticket();}public static long consumptionGeneration(){return NativeConsumption.generation();}
 public static double consumptionPortion(){return NativeConsumption.portion();}public static String consumptionStatus(){return NativeConsumption.status()+"; "+BiteRenderHook.status();}
 public static boolean consumptionAcknowledge(long ticket,boolean queued){return NativeConsumption.acknowledge(ticket,queued);}
 public static boolean consumptionValid(IsoPlayer p,InventoryItem item,int side,long ticket,long epoch){try{return side>=0&&side<=1&&NativeConsumption.valid(p,item,side,ticket,epoch);}catch(RuntimeException|LinkageError invalid){return false;}}
 public static void consumptionFinished(long ticket){NativeConsumption.finish(ticket);}
 public static String tapsConfigure(boolean value){try{if(value&&!ready)return "Unavailable: "+failure;NativeTaps.configure(value);return NativeTaps.status();}catch(RuntimeException|LinkageError error){configurationErrors.incrementAndGet();return "Taps unavailable: "+error;}}
 public static void tapsTick(){try{NativeTaps.tick();}catch(RuntimeException|LinkageError error){NativeTaps.configure(false);System.err.println("[PZVR Physicality] Taps disabled: "+error);}}
 private static NativeTaps.Request t(){return NativeTaps.request();}
 public static int tapsAction(){return t()==null?0:t().cup()==null?1:2;}public static IsoPlayer tapsPlayer(){return t()==null?null:t().actor();}
 public static zombie.iso.IsoObject tapsSource(){return t()==null?null:t().source();}public static InventoryItem tapsCup(){return t()==null?null:t().cup();}
 public static long tapsTicket(){return t()==null?0:t().ticket();}public static long tapsGeneration(){return t()==null?0:t().epoch();}
 public static boolean tapsAcknowledge(long ticket,boolean queued){return NativeTaps.acknowledge(ticket,queued);}
 public static boolean tapsValid(long ticket,long epoch){try{return NativeTaps.valid(ticket,epoch);}catch(RuntimeException|LinkageError invalid){return false;}}
 public static void tapsFinished(long ticket){NativeTaps.finished(ticket);}public static String tapsStatus(){return NativeTaps.status();}
 private Main(){}
}
