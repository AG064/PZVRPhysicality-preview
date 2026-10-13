package pzvrphysicality;

import java.util.ArrayList;
import local.pzvr.Engine;
import local.pzvr.RigContacts;
import local.pzvr.XrBackend;

/** Borrows PZVR's existing tracking/contact publication, never its renderer or input ownership. */
public final class PzvrContacts {
 public static ContactFrame sample(long now){
  var frame=Engine.latestInput;var rig=RigContacts.latest;
  if(!Engine.active()||!Engine.worldPresent||frame==null||!frame.focused()||rig==null
    ||now<rig.hostTime()||now-rig.hostTime()>=150_000_000L||frame.time()<rig.inputTime()
    ||rig.inputTime()<0||frame.time()<0||frame.time()-rig.inputTime()>150_000_000L
    ||rig.hands()==null||rig.hands().size()>2)return null;
  var hands=new ArrayList<ContactFrame.Hand>();
  try{for(var source:rig.hands()){
   if(source==null)return null;int side=source.hand();if(side<0||side>1)return null;
   var input=side==0?frame.left():frame.right();var volume=source.volume();
   if(input==null||!input.tracked()||input.grip()==null||volume==null)continue;
   // Upstream rig vectors are (x, physical height, y). No repeated eye/head/recenter transform.
   var start=volume.start();var end=volume.end();if(start==null||end==null)return null;
   hands.add(new ContactFrame.Hand(side,new ContactFrame.Point(start.x(),start.z(),start.y()),
     new ContactFrame.Point(end.x(),end.z(),end.y()),volume.radius(),input.down(XrBackend.GRIP),input.down(XrBackend.TRIGGER)));
  }}catch(IllegalArgumentException invalid){return null;}
  try{return hands.isEmpty()?null:new ContactFrame(rig.hostTime(),rig.inputTime(),hands);}
  catch(IllegalArgumentException invalid){return null;}
 }
 private PzvrContacts(){}
}
