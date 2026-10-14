package pzvrphysicality;

import java.util.ArrayList;
import local.pzvr.Engine;
import local.pzvr.RigContacts;
import local.pzvr.XrBackend;

/** Borrows PZVR's existing tracking/contact publication, never its renderer or input ownership. */
public final class PzvrContacts {
 private record Readback(String reason,long checked,long rigTime,long inputTime,int hands){}
 private static volatile Readback last=new Readback("not sampled",0,0,0,0);
 private static ContactFrame rejected(String why,long now,local.pzvr.XrBackend.Frame input,local.pzvr.RigContacts.Snapshot rig){last=new Readback(why,now,rig==null?0:rig.hostTime(),input==null?0:input.time(),0);return null;}
 public static String status(){var value=last;long now=System.nanoTime();return "Contacts "+value.reason()+"; hands="+value.hands()+" rigAgeMs="+age(now,value.rigTime())+" checkedAgeMs="+age(now,value.checked());}
 private static String age(long now,long time){return time==0?"?":now<time?"future":Long.toString((now-time)/1_000_000L);}
 public static ContactFrame sample(long now){
  var frame=Engine.latestInput;var rig=RigContacts.latest;
  if(!Engine.active()||!Engine.worldPresent)return rejected("XR/world inactive",now,frame,rig);
  if(frame==null||!frame.focused())return rejected("sampled input absent/unfocused",now,frame,rig);
  if(rig==null)return rejected("native rig not published",now,frame,rig);
  if(now<rig.hostTime()||now-rig.hostTime()>=150_000_000L||frame.time()<rig.inputTime()
    ||rig.inputTime()<0||frame.time()<0||frame.time()-rig.inputTime()>150_000_000L
    ||rig.hands()==null||rig.hands().size()>2)return rejected("native rig stale/malformed",now,frame,rig);
  var hands=new ArrayList<ContactFrame.Hand>();
  try{for(var source:rig.hands()){
   if(source==null)return rejected("native hand absent",now,frame,rig);int side=source.hand();if(side<0||side>1)return rejected("native hand side invalid",now,frame,rig);
   var input=side==0?frame.left():frame.right();var volume=source.volume();
   if(input==null||!input.tracked()||input.grip()==null||volume==null)continue;
   // Upstream rig vectors are (x, physical height, y). No repeated eye/head/recenter transform.
   var start=volume.start();var end=volume.end();if(start==null||end==null)return rejected("native contact volume absent",now,frame,rig);
   hands.add(new ContactFrame.Hand(side,new ContactFrame.Point(start.x(),start.z(),start.y()),
     new ContactFrame.Point(end.x(),end.z(),end.y()),volume.radius(),input.down(XrBackend.GRIP),input.down(XrBackend.TRIGGER)));
  }}catch(IllegalArgumentException invalid){return rejected("native hand numeric data invalid",now,frame,rig);}
  try{if(hands.isEmpty())return rejected("no tracked native contact hands",now,frame,rig);var result=new ContactFrame(rig.hostTime(),rig.inputTime(),hands);last=new Readback("fresh",now,rig.hostTime(),frame.time(),hands.size());return result;}
  catch(IllegalArgumentException invalid){return rejected("native contact frame invalid",now,frame,rig);}
 }
 private PzvrContacts(){}
}
