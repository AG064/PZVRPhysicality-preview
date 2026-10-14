package pzvrphysicality;

import java.lang.reflect.Field;import java.util.*;
import local.pzvr.*;import viewpoint.core.Frame;import viewpoint.render.ModelDraws;
import zombie.characters.IsoPlayer;import zombie.core.skinnedmodel.model.ModelInstance;import zombie.scripting.objects.ModelAttachment;

/** Bounded read-only placement receipt. Native attachments and draw packets are never changed. */
public final class ItemModelDiagnostics {
 private static volatile boolean enabled;private static volatile long epoch,next,last;private static volatile String receipt="No held model receipt";private static Field instances,values;
 public static void configure(boolean value){if(value&&!Main.ready())throw new IllegalStateException("SDK validation pending");enabled=value;epoch++;next=last=0;receipt=value?"Held model receipts armed":"Held model receipts Off";}
 public static boolean enabled(){return enabled;}
 public static String status(){return receipt+(enabled&&last!=0&&System.nanoTime()-last>1_000_000_000L?"; stale receipt":"");}
 public static void capture(Frame frame){capture(frame,System.nanoTime());}
 static void capture(Frame frame,long now){
  if(!enabled||!Main.ready()||frame==null||now<next||IsoPlayer.players.length==0)return;
  long mode=epoch;var actor=IsoPlayer.players[0];var input=Engine.latestInput;
  if(actor==null||input==null||!input.focused()||NativeRig.posedAt<=0||now<NativeRig.posedAt||now-NativeRig.posedAt>150_000_000L)return;
  next=now+250_000_000L;
  try{
   if(instances==null){instances=field("instances");values=field("values");}
   var draws=frame.scene.models;int count=draws.count();var objects=(ModelInstance[])instances.get(draws);var rows=(float[])values.get(draws);
   if(count<0||count>4096||count>objects.length||count>rows.length/68)throw new IllegalArgumentException("Held diagnostic packet extent");
   var text=new StringBuilder("Held placement receipt; raw controller metres and native render matrices; ");
   for(int side=0;side<2;side++){
    var item=side==0?actor.getSecondaryHandItem():actor.getPrimaryHandItem();var model=side==0?actor.secondaryHandModel:actor.primaryHandModel;var hand=side==0?input.left():input.right();
    text.append(side==0?"L":"R").append(" item=").append(item==null?"empty":item.getFullType()+" index="+item.getModelIndex());
    if(hand!=null){text.append(" grip=").append(point(hand.grip())).append(" aim=").append(point(hand.aim()));}
    if(model==null){text.append(" model=none; ");continue;}
    text.append(" script=").append(model.modelScript==null?"none":model.modelScript.getName()).append(" instanceScale=").append(model.scale).append(" parentBone=").append(model.parentBoneName).append(" parentAttachment=").append(model.attachmentNameParent).append(" selfAttachment=").append(model.attachmentNameSelf);
    var parent=model.parent;var pa=parent==null?null:parent.getAttachmentById(model.attachmentNameParent);if(pa==null&&parent!=null&&model.parentBoneName!=null)pa=parent.getAttachmentById(model.parentBoneName);
    var sa=model.getAttachmentById(model.attachmentNameSelf);if(sa==null&&model.parentBoneName!=null)sa=model.getAttachmentById(model.parentBoneName);
    text.append(" selectedParent=").append(attachment(pa)).append(" selectedSelf=").append(attachment(sa)).append(" selfProp1=").append(attachment(model.getAttachmentById("Bip01_Prop1"))).append(" selfProp2=").append(attachment(model.getAttachmentById("Bip01_Prop2")));
    int found=-1,hits=0;for(int n=0;n<count;n++)if(objects[n]==model){found=n;hits++;}
    text.append(" rows=").append(hits);if(hits==1)text.append(" renderMatrix=").append(Arrays.toString(Arrays.copyOfRange(rows,found*68,found*68+16)));text.append("; ");
   }
   if(enabled&&mode==epoch&&input==Engine.latestInput&&input.focused()&&actor==IsoPlayer.players[0]){receipt=text.toString();last=now;}
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){if(enabled&&mode==epoch){receipt="Held model receipt unavailable: "+unsupported.getClass().getSimpleName();last=now;}}
 }
 private static String point(Math3.Pose pose){return pose==null?"none":pose.position().toString();}
 private static String attachment(ModelAttachment value){return value==null?"none":value.getId()+" offset="+value.getOffset()+" rotate="+value.getRotate()+" scale="+value.getScale();}
 private static Field field(String name)throws ReflectiveOperationException{var f=ModelDraws.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private ItemModelDiagnostics(){}
}
