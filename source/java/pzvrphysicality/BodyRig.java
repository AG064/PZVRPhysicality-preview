package pzvrphysicality;

/** Gravity aligned body coordinates derived from the original tracked head, without changing its pose. */
public record BodyRig(WorldRigFrame rig,ContactFrame.Point head,ContactFrame.Point right,ContactFrame.Point forward,double units) {
 public BodyRig {
  if(rig==null||head==null||right==null||forward==null||!Double.isFinite(units)||units<.25||units>4)throw new IllegalArgumentException("Body rig");
  if(Math.abs(Math.hypot(right.x(),right.y())-1)>.001||Math.abs(Math.hypot(forward.x(),forward.y())-1)>.001||Math.abs(right.height())>.001||Math.abs(forward.height())>.001||Math.abs(right.x()*forward.x()+right.y()*forward.y())>.001)throw new IllegalArgumentException("Body basis");
 }
 public static BodyRig from(WorldRigFrame rig,ContactFrame.Point head,ContactFrame.Point right,ContactFrame.Point forward,double units){
  double length=Math.hypot(right.x(),right.y());if(length<units*.1)return null;
  var r=new ContactFrame.Point(right.x()/length,right.y()/length,0);var f=new ContactFrame.Point(-r.y(),r.x(),0);
  if(f.x()*forward.x()+f.y()*forward.y()<0)f=new ContactFrame.Point(-f.x(),-f.y(),0);
  return new BodyRig(rig,head,r,f,units);
 }
 public ContactFrame.Point at(double x,double y,double z){return new ContactFrame.Point(head.x()+units*(right.x()*x+forward.x()*y),head.y()+units*(right.y()*x+forward.y()*y),head.height()+units*z);}
 public double[] local(ContactFrame.Point p){double x=(p.x()-head.x())/units,y=(p.y()-head.y())/units;return new double[]{x*right.x()+y*right.y(),x*forward.x()+y*forward.y(),(p.height()-head.height())/units};}
}
