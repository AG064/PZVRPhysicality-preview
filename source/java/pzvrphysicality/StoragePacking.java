package pzvrphysicality;

import java.util.*;

/** Bounded translations on an existing support plane. Native items, scale and orientation stay unchanged. */
public final class StoragePacking {
 public static final int MAX_OCCUPIED=64,MAX_AXIS=16,MAX_CANDIDATES=96,MAX_GEOMETRY_QUERIES=128;
 public static final class Budget {
  private final int limit;private int used;
  public Budget(int limit){if(limit<0||limit>1024)throw new IllegalArgumentException("Packing geometry budget");this.limit=limit;}
  private boolean take(){if(used>=limit)return false;used++;return true;}
  public int used(){return used;}
 }
 public record Result(ContactFrame.Point position,boolean exhausted,int candidates){}
 private record Candidate(double x,double d){}
 public static StorageFrame.Bounds translated(StorageFrame.Bounds bounds,double x,double d,double h){return new StorageFrame.Bounds(bounds.minX()+x,bounds.maxX()+x,bounds.minD()+d,bounds.maxD()+d,bounds.minH()+h,bounds.maxH()+h);}
 public static boolean overlaps(StorageFrame.Bounds a,StorageFrame.Bounds b,double gap){
  return a.minX()<b.maxX()+gap&&a.maxX()>b.minX()-gap&&a.minD()<b.maxD()+gap&&a.maxD()>b.minD()-gap&&a.minH()<b.maxH()+gap&&a.maxH()>b.minH()-gap;
 }
 public static boolean free(StorageFrame.Bounds bounds,List<StorageFrame.Bounds> occupied,double gap){for(var other:occupied)if(overlaps(bounds,other,gap))return false;return true;}
 private static List<Double> axis(double low,double high,double itemLow,double itemHigh,List<StorageFrame.Bounds> occupied,boolean depth,double inset,double gap){
  var values=new TreeSet<Double>();values.add(0d);values.add(low+inset-itemLow);values.add(high-inset-itemHigh);
  for(var other:occupied){values.add((depth?other.minD():other.minX())-gap-itemHigh);values.add((depth?other.maxD():other.maxX())+gap-itemLow);}
  return values.stream().filter(Double::isFinite).sorted(Comparator.comparingDouble((Double n)->Math.abs(n)).thenComparingDouble(Double::doubleValue)).limit(MAX_AXIS).toList();
 }
 public static Result place(StorageFrame frame,StorageOcclusion housing,ContactFrame.Point pivot,StorageFrame.Bounds item,List<StorageFrame.Bounds> occupied,Budget budget){
  if(frame==null||housing==null||pivot==null||item==null||occupied==null||budget==null||occupied.size()>MAX_OCCUPIED||occupied.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("Packing inputs");
  double inset=.002/frame.scale(),gap=.004/frame.scale(),height=item.maxH()-item.minH();
  if(item.maxX()-item.minX()>frame.maxX()-frame.minX()-2*inset||item.maxD()-item.minD()>frame.maxD()-frame.minD()-2*inset||height<=0||height>frame.maxH()-frame.minH()-2*inset)return new Result(null,false,0);
  if(budget.used>=budget.limit)return new Result(null,true,0);
  var candidates=new ArrayList<Candidate>();
  for(double x:axis(frame.minX(),frame.maxX(),item.minX(),item.maxX(),occupied,false,inset,gap))for(double d:axis(frame.minD(),frame.maxD(),item.minD(),item.maxD(),occupied,true,inset,gap))candidates.add(new Candidate(x,d));
  candidates.sort(Comparator.comparingDouble((Candidate c)->c.x*c.x+c.d*c.d).thenComparingDouble(Candidate::x).thenComparingDouble(Candidate::d));
  var local=frame.local(pivot);int tried=0;
  for(var candidate:candidates){
   if(tried++>=MAX_CANDIDATES)break;
   var moved=translated(item,candidate.x,candidate.d,0);
   if(moved.minX()<frame.minX()+inset-1e-8||moved.maxX()>frame.maxX()-inset+1e-8||moved.minD()<frame.minD()+inset-1e-8||moved.maxD()>frame.maxD()-inset+1e-8||!free(moved,occupied,gap))continue;
   double edgeX=Math.min(inset,(moved.maxX()-moved.minX())*.1),edgeD=Math.min(inset,(moved.maxD()-moved.minD())*.1);
   double[] xs={(moved.minX()+moved.maxX())/2,moved.minX()+edgeX,moved.maxX()-edgeX,moved.minX()+edgeX,moved.maxX()-edgeX};
   double[] ds={(moved.minD()+moved.maxD())/2,moved.minD()+edgeD,moved.minD()+edgeD,moved.maxD()-edgeD,moved.maxD()-edgeD};
   double support=Double.NaN;boolean supported=true;
   for(int n=0;n<xs.length;n++){
    if(!budget.take())return new Result(null,true,tried);
    double surface=housing.support(xs[n],ds[n],item.minH()+.01/frame.scale());
    if(!Double.isFinite(surface)||surface<frame.minH()-1e-8||Math.abs(surface-(item.minH()-inset))>.001/frame.scale()||Double.isFinite(support)&&Math.abs(support-surface)>.001/frame.scale()){supported=false;break;}
    support=surface;
   }
   if(!supported)continue;
   double shift=support+inset-item.minH();moved=translated(moved,0,0,shift);
   if(moved.maxH()>frame.maxH()-inset+1e-8||!free(moved,occupied,gap))continue;
   for(int n=0;n<xs.length;n++){
    if(!budget.take())return new Result(null,true,tried);
    if(housing.blocked(new ContactFrame.Point(xs[n],ds[n],moved.minH()+inset),new ContactFrame.Point(xs[n],ds[n],moved.maxH()-Math.min(inset,height*.1)))){supported=false;break;}
   }
   if(supported)return new Result(frame.world(local.x()+candidate.x,local.y()+candidate.d,local.height()+shift),false,tried);
  }
  return new Result(null,false,Math.min(tried,MAX_CANDIDATES));
 }
 private StoragePacking(){}
}
