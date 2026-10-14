package zombie.core.logger;
/** Offline interpreter exceptions never invoke native UI/log initialization. */
public final class ExceptionLogger {
 public static void logException(Throwable error){System.err.println("[Authored Lua fixture] "+error);}
 public static void logException(Throwable error,String context){System.err.println("[Authored Lua fixture] "+context+": "+error);}
 private ExceptionLogger(){}
}
