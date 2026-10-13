package pzvrphysicality;

import java.util.List;

/** Numeric world contacts published from the same PZVR rig capture as the visible hands. */
public record WorldRigFrame(long hostTime,long inputTime,Object actor,Object scene,ContactFrame.Point camera,long generation,ContactFrame.Point mouth,List<Hand> hands) {
 public record Hand(int side,ContactFrame.Point grip,ContactFrame.Point right,ContactFrame.Point up,ContactFrame.Point forward,boolean trigger){
  public Hand{if(side<0||side>1||grip==null||right==null||up==null||forward==null)throw new IllegalArgumentException("World rig hand");
   double a=length(right),b=length(up),c=length(forward);if(a<.25||a>4||Math.abs(a-b)>.002||Math.abs(a-c)>.002)throw new IllegalArgumentException("World rig scale");
   if(Math.abs(dot(right,up))>a*a*.005||Math.abs(dot(right,forward))>a*a*.005||Math.abs(dot(up,forward))>a*a*.005)throw new IllegalArgumentException("World rig basis");
  }
  public double units(){return length(right);}
  public ContactFrame.Point point(MouthMath.Vec local){return new ContactFrame.Point(grip.x()+right.x()*local.x()+up.x()*local.y()+forward.x()*local.z(),grip.y()+right.y()*local.x()+up.y()*local.y()+forward.y()*local.z(),grip.height()+right.height()*local.x()+up.height()*local.y()+forward.height()*local.z());}
  public double turnDegrees(){return Math.hypot(right.x(),right.y())<units()*.1?Double.NaN:Math.toDegrees(Math.atan2(right.y(),right.x()));}
 }
 public WorldRigFrame{hands=List.copyOf(hands);if(actor==null||scene==null||camera==null||mouth==null||generation<0||hands.size()>2||hands.stream().map(Hand::side).distinct().count()!=hands.size())throw new IllegalArgumentException("World rig publication");}
 public boolean fresh(long now){return now>=hostTime&&now-hostTime<150_000_000L;}
 public Hand hand(int side){return hands.stream().filter(h->h.side==side).findFirst().orElse(null);}
 public static ContactFrame.Point fromRender(double camX,double camY,double camZ,MouthMath.Vec offset){return new ContactFrame.Point(camX-offset.x(),camY-offset.z(),camZ*Math.sqrt(6)+offset.y());}
 private static double dot(ContactFrame.Point a,ContactFrame.Point b){return a.x()*b.x()+a.y()*b.y()+a.height()*b.height();}
 private static double length(ContactFrame.Point a){return Math.sqrt(dot(a,a));}
}
