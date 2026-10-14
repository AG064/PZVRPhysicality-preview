package zombie.Lua;
/** Interpreter bookkeeping only. This is never the native Lua manager. */
public final class LuaManager {
 public static se.krka.kahlua.vm.KahluaThread thread,debugthread;
 public static se.krka.kahlua.integration.LuaCaller debugcaller;
 public static se.krka.kahlua.vm.KahluaTable env;
 private LuaManager(){}
 public static StackTraceElement[] getLuaStackStrace(se.krka.kahlua.vm.Coroutine coroutine){return new StackTraceElement[0];}
}
