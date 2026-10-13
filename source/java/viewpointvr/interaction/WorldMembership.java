package viewpointvr.interaction;

/** Transactional membership migration without constructing or cloning an item. */
public final class WorldMembership {
 public interface Graph<T,S>{
  S square(T value);void square(T value,S square);
  int index(S square,T value,boolean world);int size(S square,boolean world);
  void remove(S square,T value,boolean world);void add(S square,int index,T value,boolean world);
 }
 private WorldMembership(){}
 public static <T,S> boolean move(T value,S from,S to,Graph<T,S> graph,Runnable place,Runnable restore){
  int world=graph.index(from,value,true),ordinary=graph.index(from,value,false);
  if(graph.square(value)!=from||world<0||from!=to&&(graph.index(to,value,true)>=0||graph.index(to,value,false)>=0))return false;
  try{
   if(from!=to){graph.remove(from,value,true);if(ordinary>=0)graph.remove(from,value,false);graph.square(value,to);graph.add(to,graph.size(to,true),value,true);if(ordinary>=0)graph.add(to,graph.size(to,false),value,false);}
   place.run();return true;
  }catch(RuntimeException error){
   try{
    if(from!=to){graph.remove(to,value,true);graph.remove(to,value,false);graph.square(value,from);
     if(graph.index(from,value,true)<0)graph.add(from,Math.min(world,graph.size(from,true)),value,true);
     if(ordinary>=0&&graph.index(from,value,false)<0)graph.add(from,Math.min(ordinary,graph.size(from,false)),value,false);
    }restore.run();
   }catch(RuntimeException rollback){error.addSuppressed(rollback);}
   throw error;
  }
 }
}
