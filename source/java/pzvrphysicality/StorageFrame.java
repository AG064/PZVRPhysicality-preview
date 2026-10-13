package pzvrphysicality;
import org.joml.Quaternionf;

/** Numeric pack-local interior mapping. Item scale is retained; an exterior bound is not a collider. */
public record StorageFrame(ContactFrame.Point origin,float cos,float sin,float scale,float minX,float maxX,float minD,float maxD,float minH,float maxH) {
 public record Bounds(double minX,double maxX,double minD,double maxD,double minH,double maxH){
  public Bounds {for(double n:new double[]{minX,maxX,minD,maxD,minH,maxH})if(!Double.isFinite(n))throw new IllegalArgumentException("Storage item bounds");if(minX>maxX||minD>maxD||minH>maxH)throw new IllegalArgumentException("Storage item extent");}
  public double distance(ContactFrame.Point p){double x=Math.max(0,Math.max(minX-p.x(),p.x()-maxX)),y=Math.max(0,Math.max(minD-p.y(),p.y()-maxD)),h=Math.max(0,Math.max(minH-p.height(),p.height()-maxH));return Math.sqrt(x*x+y*y+h*h);}
 }
 public StorageFrame {
  if(origin==null)throw new IllegalArgumentException("Storage origin");
  for(float f:new float[]{cos,sin,scale,minX,maxX,minD,maxD,minH,maxH})if(!Float.isFinite(f))throw new IllegalArgumentException("Storage frame number");
  if(Math.abs(cos*cos+sin*sin-1)>.002||scale<=0||scale>16||(maxX-minX)*scale<.08||(maxD-minD)*scale<.04||(maxH-minH)*scale<.08
   ||(maxX-minX)*scale>4||(maxD-minD)*scale>4||(maxH-minH)*scale>4)throw new IllegalArgumentException("Storage frame extent");
 }
 public ContactFrame.Point world(double x,double d,double h){return new ContactFrame.Point(origin.x()+scale*(-x*cos+d*sin),origin.y()+scale*(-x*sin-d*cos),origin.height()+scale*h);}
 public ContactFrame.Point local(ContactFrame.Point p){double x=(p.x()-origin.x())/scale,y=(p.y()-origin.y())/scale;return new ContactFrame.Point(-x*cos-y*sin,x*sin-y*cos,(p.height()-origin.height())/scale);}
 public ContactFrame.Point position(double x,double y,double z){
  for(double f:new double[]{x,y,z})if(!Double.isFinite(f)||f<0||f>1)throw new IllegalArgumentException("Normalized storage pose");
  double inset=.02/scale;
  return world(minX+inset+(maxX-minX-2*inset)*x,minD+inset+(maxD-minD-2*inset)*y,minH+inset+(maxH-minH-2*inset)*z);
 }
 public ContactFrame.Point rawPosition(double x,double y,double z){
  for(double f:new double[]{x,y,z})if(!Double.isFinite(f)||f<-.05||f>1.05)throw new IllegalArgumentException("Normalized raw storage pose");
  return world(minX+(maxX-minX)*x,minD+(maxD-minD)*y,minH+(maxH-minH)*z);
 }
 public float[] rotation(float x,float y,float z,float w){
  double n=x*x+y*y+z*z+w*w;if(!Double.isFinite(n)||Math.abs(n-1)>.002)throw new IllegalArgumentException("Storage rotation");
  var parent=new Quaternionf().rotationZ((float)Math.atan2(-sin,-cos));var q=parent.mul(new Quaternionf(x,y,z,w)).normalize();return new float[]{q.x,q.y,q.z,q.w};
 }
 public ContactFrame.Point fit(ContactFrame.Point pivot,Bounds item){
  double inset=.002/scale;double width=maxX-minX-2*inset,depth=maxD-minD-2*inset,height=maxH-minH-2*inset;
  if(item.maxX-item.minX>width||item.maxD-item.minD>depth||item.maxH-item.minH>height)return null;
  double x=Math.max(0,minX+inset-item.minX)+Math.min(0,maxX-inset-item.maxX);
  double d=Math.max(0,minD+inset-item.minD)+Math.min(0,maxD-inset-item.maxD);
  double h=Math.max(0,minH+inset-item.minH)+Math.min(0,maxH-inset-item.maxH);
  var local=local(pivot);return world(local.x()+x,local.y()+d,local.height()+h);
 }
}
