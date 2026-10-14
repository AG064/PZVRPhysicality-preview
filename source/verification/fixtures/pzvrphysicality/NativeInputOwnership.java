package pzvrphysicality;
/** Authored legacy advice fixture. Ownership semantics have their dedicated production suite. */
public final class NativeInputOwnership {
 public static int bindingSide=1,opens,closes,observations;public static boolean left=true,right=true,fail;
 public record Snapshot(){public boolean allows(int side){return side==0?left:side==1&&right;}public boolean current(){return !fail;}public boolean globalBlocked(){return fail||!left&&!right;}public long generation(int side){return side>=0&&side<2?1:-1;}}
 public static final class Scope implements AutoCloseable {public Snapshot snapshot(){return new Snapshot();}public void close(){closes++;}}
 public static Scope open(){opens++;if(fail)throw new IllegalStateException("Authored unavailable owner");return new Scope();}
 public static Snapshot read(){return new Snapshot();}public static boolean allows(int side){return read().allows(side);}public static boolean globalBlocked(){return read().globalBlocked();}public static long generation(int side){return read().generation(side);}public static void observe(Throwable failure){observations++;}
 public static int bindingHandIndex(){return bindingSide;}
 private NativeInputOwnership(){}
}
