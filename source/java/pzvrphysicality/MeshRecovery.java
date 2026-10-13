package pzvrphysicality;

import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import viewpointvr.physics.PackedModelGeometry;

/** Recovers missing canonical CPU triangles without requesting textures or changing native residency. */
public final class MeshRecovery {
 private static final int MAX_BYTES=2*1024*1024,MAX_LINE=8192,MAX_LINES=100000;
 private record Attempt(long epoch,long retry,boolean running){}
 private static final Map<Object,Attempt> attempts=new WeakHashMap<>();
 private static final ThreadPoolExecutor reader=new ThreadPoolExecutor(0,1,10,TimeUnit.SECONDS,new ArrayBlockingQueue<>(4),task->{var thread=new Thread(task,"PZVR physicality mesh reader");thread.setDaemon(true);return thread;},new ThreadPoolExecutor.AbortPolicy());
 private static int recovered,failures;private static long warning;
 private MeshRecovery(){}
 public static synchronized void request(Object model){
  if(model==null||PackedModelGeometry.get(model)!=null)return;long now=System.nanoTime(),epoch=NativeItems.epoch();var previous=attempts.get(model);
  if(previous!=null&&(previous.running||previous.epoch==epoch&&now<previous.retry))return;
  attempts.entrySet().removeIf(entry->!entry.getValue().running&&entry.getValue().retry<now);if(attempts.size()>=128&&!attempts.containsKey(model))return;
  var attempt=new Attempt(epoch,now,true);attempts.put(model,attempt);var weak=new WeakReference<>(model);
  try{reader.execute(()->recover(weak,attempt));}catch(RejectedExecutionException busy){attempts.put(model,new Attempt(epoch,now+500_000_000L,false));}
 }
 private static List<String> lines(Path file)throws IOException{
  var result=new ArrayList<String>();int characters=0,previous=-1;var line=new StringBuilder();
  // Bound each line while reading. Checking after readLine could allocate an
  // arbitrarily long line if a source grows after the metadata preflight.
  try(var input=Files.newBufferedReader(file,StandardCharsets.UTF_8)){int character;
   while((character=input.read())!=-1){if(++characters>MAX_BYTES)throw new IOException("Packed source text budget");if(character=='\r'||character=='\n'){if(character!='\n'||previous!='\r'){if(result.size()>=MAX_LINES)throw new IOException("Packed source line budget");result.add(line.toString());line.setLength(0);}}else{if(line.length()>=MAX_LINE)throw new IOException("Packed source line budget");line.append((char)character);}previous=character;}
   if(!line.isEmpty()){if(result.size()>=MAX_LINES)throw new IOException("Packed source line budget");result.add(line.toString());}
  }
  int vertices=0,indices=0;for(var value:result){var clean=value.stripLeading();if(clean.startsWith("v ")||clean.startsWith("v\t")){if(++vertices>32000)throw new IOException("Packed source vertex budget");}if(clean.startsWith("f ")||clean.startsWith("f\t")){int corners=clean.substring(1).trim().split("\\s+").length;if(corners<3||corners>2048)throw new IOException("Packed source face budget");indices+=(corners-2)*3;if(indices>192000)throw new IOException("Packed source triangle budget");}}
  return result;
 }
 private static void recover(WeakReference<Object> weak,Attempt attempt){
  Object model=weak.get();if(model==null)return;boolean successful=false;Throwable problem=null;
  try{
   if(attempt.epoch!=NativeItems.epoch()||PackedModelGeometry.get(model)!=null)return;
   var type=model.getClass();if(!type.getName().equals("viewpoint.render.PackModel"))throw new IllegalArgumentException("Packed model owner");
   int phase=(int)type.getMethod("state").invoke(model);if(phase!=2&&phase!=3)return;
   var source=(File)type.getField("obj").get(model);if(source==null||!source.getName().toLowerCase(Locale.ROOT).endsWith(".obj"))throw new IOException("Packed source filename");
   var file=source.toPath();var before=Files.readAttributes(file,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);if(!before.isRegularFile()||before.size()<=0||before.size()>MAX_BYTES)throw new IOException("Packed source file budget");
   var text=lines(file);var loader=type.getClassLoader();var parser=Class.forName("viewpoint.packs.ObjReader",false,loader);var parse=parser.getDeclaredMethod("parse",List.class);parse.setAccessible(true);var mesh=parse.invoke(null,text);
   var vertices=mesh.getClass().getDeclaredMethod("vertices");var indices=mesh.getClass().getDeclaredMethod("indices");vertices.setAccessible(true);indices.setAccessible(true);int stride=Class.forName("viewpoint.render.PackData",false,loader).getField("VERTEX_FLOATS").getInt(null);
   var points=(float[])vertices.invoke(mesh);var faces=(int[])indices.invoke(mesh);if(!matches(points,stride,(float[])type.getMethod("bounds").invoke(model)))throw new IOException("Packed source differs from resident bounds");var after=Files.readAttributes(file,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
   if(!after.isRegularFile()||before.size()!=after.size()||!before.lastModifiedTime().equals(after.lastModifiedTime())||!Objects.equals(before.fileKey(),after.fileKey()))throw new IOException("Packed source changed during recovery");
   if(attempt.epoch!=NativeItems.epoch()||(int)type.getMethod("state").invoke(model)!=phase)return;
   successful=PackedModelGeometry.recover(model,points,faces,stride);
   if(!successful)throw new IOException("Packed recovered geometry rejected");
  }catch(ReflectiveOperationException|RuntimeException|IOException error){problem=error instanceof InvocationTargetException invocation&&invocation.getCause()!=null?invocation.getCause():error;}
  finally{
   synchronized(MeshRecovery.class){if(attempts.get(model)==attempt)attempts.put(model,new Attempt(attempt.epoch,System.nanoTime()+5_000_000_000L,false));if(successful)recovered++;else if(problem!=null){failures++;long now=System.nanoTime();if(now-warning>5_000_000_000L){warning=now;System.err.println("[PZVR Physicality] Exact mesh unavailable: "+problem.getClass().getSimpleName());}}}
  }
 }
 private static boolean matches(float[] points,int stride,float[] bounds){
  if(points==null||stride<3||stride>16||points.length%stride!=0||points.length/stride<4||points.length/stride>32000||bounds==null||bounds.length!=6)return false;
  var found=new float[]{Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
  for(int i=0;i<points.length;i+=stride)for(int axis=0;axis<3;axis++){float value=points[i+axis];if(!Float.isFinite(value))return false;found[axis]=Math.min(found[axis],value);found[axis+3]=Math.max(found[axis+3],value);}
  for(int i=0;i<6;i++)if(!Float.isFinite(bounds[i])||Math.abs(found[i]-bounds[i])>Math.max(.0001f,Math.abs(bounds[i])*.0001f))return false;return true;
 }
 public static synchronized boolean pending(Object model){var attempt=attempts.get(model);return attempt!=null&&attempt.running;}
 public static synchronized String status(){return "CPU meshes recovered "+recovered+", queued "+reader.getQueue().size()+", failed reads "+failures;}
}
