package pzvrphysicality;
import java.lang.reflect.Field;
import java.util.ArrayList;

/** Borrows the existing producer destination. Its original drawer lifecycle owns every appended resource. */
public final class StorageCaptureLease implements AutoCloseable {
 private final Field flag,sink;private final Object previousSink;private final boolean previousFlag;
 private final Thread owner=Thread.currentThread();private boolean closed;
 public StorageCaptureLease(Field flag,Field sink,ArrayList<?> drawers)throws IllegalAccessException{
  if(flag.getType()!=boolean.class||sink.getType()!=ArrayList.class||drawers==null)throw new IllegalArgumentException("Storage capture schema");
  this.flag=flag;this.sink=sink;previousFlag=flag.getBoolean(null);previousSink=sink.get(null);sink.set(null,drawers);
  try{flag.setBoolean(null,true);}catch(IllegalAccessException|RuntimeException error){sink.set(null,previousSink);throw error;}
 }
 @Override public void close()throws IllegalAccessException{
  if(Thread.currentThread()!=owner)throw new IllegalStateException("Storage capture owner changed");if(closed)return;closed=true;
  try{flag.setBoolean(null,previousFlag);}finally{sink.set(null,previousSink);}
 }
}
