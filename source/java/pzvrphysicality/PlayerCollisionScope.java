package pzvrphysicality;
import java.util.ArrayDeque;
/** Primitive balanced scope survives Off transitions and exceptions without eager SDK adapter loading. */
public final class PlayerCollisionScope {
 private static final ThreadLocal<ArrayDeque<Boolean>> scopes=new ThreadLocal<>();private static final ThreadLocal<Integer> overflow=new ThreadLocal<>();
 public static void enter(){var values=scopes.get();boolean enabled=RouteModes.playerCollision();if(!enabled&&values==null)return;if(values==null){values=new ArrayDeque<>();scopes.set(values);}if(values.size()>=64){var extra=overflow.get();overflow.set(extra==null?1:extra+1);return;}if(enabled)NativePlayerCollision.roomEnter();values.addLast(enabled);}
 public static void exit(){var extra=overflow.get();if(extra!=null){if(extra<=1)overflow.remove();else overflow.set(extra-1);return;}var values=scopes.get();if(values==null)return;boolean entered=Boolean.TRUE.equals(values.pollLast());if(values.isEmpty())scopes.remove();if(entered)NativePlayerCollision.roomExit();}
 private PlayerCollisionScope(){}
}
