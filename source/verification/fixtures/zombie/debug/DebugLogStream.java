package zombie.debug;
/** Process-local test output, never the native game's log setup. */
public final class DebugLogStream extends java.io.PrintStream {
 public DebugLogStream(){super(System.out);}
 public String generateCallerPrefix(){return "authored Lua fixture";}
}
