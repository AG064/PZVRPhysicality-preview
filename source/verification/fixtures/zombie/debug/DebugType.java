package zombie.debug;
public enum DebugType {
 Lua,General;
 private static final DebugLogStream STREAM=new DebugLogStream();
 public DebugLogStream getLogStream(){return STREAM;}
 public void error(Object value){System.err.println("[Authored Lua fixture] "+value);}
 public void error(String format,Object... values){System.err.printf((format)+"%n",values);}
 public void printStackTrace(){}
}
