package pzvrphysicality;

/** Detached native claim lineage for a queued hand action. Contains no item, actor or renderer handle. */
public final class NativeActionAuthority {
 public record Stamp(int mask,long left,long right) {
  public Stamp {if(mask<1||mask>3||left<0||right<0)throw new IllegalArgumentException("Native action hand lineage");}
  public boolean current(){var snapshot=NativeInputOwnership.read();return !snapshot.globalBlocked()
   &&((mask&1)==0||snapshot.allows(0)&&snapshot.generation(0)==left)
   &&((mask&2)==0||snapshot.allows(1)&&snapshot.generation(1)==right);}
 }
 public static Stamp stamp(int side){return stamp(side,-1);}
 public static Stamp stamp(int side,int other){
  if(side<0||side>1||other< -1||other>1)return null;var snapshot=NativeInputOwnership.read();int mask=1<<side;if(other>=0)mask|=1<<other;
  if(snapshot.globalBlocked()||(mask&1)!=0&&!snapshot.allows(0)||(mask&2)!=0&&!snapshot.allows(1))return null;
  return new Stamp(mask,snapshot.generation(0),snapshot.generation(1));
 }
 public static boolean current(Stamp stamp){return stamp!=null&&stamp.current();}
 private NativeActionAuthority(){}
}
