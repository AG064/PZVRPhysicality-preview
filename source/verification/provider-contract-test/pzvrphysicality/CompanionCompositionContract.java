package pzvrphysicality;
import java.nio.file.*;import java.util.*;import java.util.jar.*;
import net.bytebuddy.dynamic.ClassFileLocator;import net.bytebuddy.pool.TypePool;
import static net.bytebuddy.matcher.ElementMatchers.*;

/** Checks current companion annotations and shared targets using copied bytes only. */
final class CompanionCompositionContract {
 record Patch(String advice,String owner,String method,String descriptor) {String target(){return owner+"."+method+descriptor;}}
 static int lastChecks,lightAdvice,weatherAdvice,sharedTargets; static final Set<String> ownTargets=new HashSet<>();
 static List<Patch> annotations(Collection<String> names,ClassFileLocator locator)throws Exception{
  var result=new ArrayList<Patch>();var pool=TypePool.Default.of(locator);
  for(String name:names)for(var annotation:pool.describe(name).resolve().getDeclaredAnnotations()){
   if(!annotation.getAnnotationType().getName().equals("me.zed_0xff.zombie_buddy.Patch"))continue;
   String owner=annotation.getValue("className").resolve(String.class),method=annotation.getValue("methodName").resolve(String.class);
   if(name.startsWith("pzvrphysicality.")&&!ownTargets.contains(owner+"."+method))continue;
   var methods=pool.describe(owner).resolve().getDeclaredMethods().filter(named(method));
   if(methods.isEmpty())throw new AssertionError("Missing current companion target "+name+" -> "+owner+"."+method);
   for(var target:methods)result.add(new Patch(name,owner,method,target.getDescriptor()));
  }
  return result;
 }
 static List<String> names(Path jar)throws Exception{try(var file=new JarFile(jar.toFile())){return Collections.list(file.entries()).stream().map(JarEntry::getName).filter(n->n.endsWith(".class")&&!n.equals("module-info.class")).map(n->n.substring(0,n.length()-6).replace('/','.')).toList();}}
 static boolean covered(Set<String> names){return names.equals(Set.of("vpinteriors.PatchCanopyCeiling","pzvrphysicality.WindowMotionCandidateAdvice"))||names.equals(Set.of("vpinteriors.PatchGather","pzvrphysicality.WorldGatherAdvice","pzvrphysicality.OwnerVisibilityGatherAdvice"));}
 public static void main(String[] a)throws Exception{
  Path output=Path.of(a[4]);var registry=new ArrayList<Patch>();
  try(var locator=new ClassFileLocator.Compound(ClassFileLocator.ForJarFile.of(Path.of(a[0]).toFile()),ClassFileLocator.ForJarFile.of(Path.of(a[1]).toFile()),ClassFileLocator.ForJarFile.of(Path.of(a[2]).toFile()),ClassFileLocator.ForJarFile.of(Path.of(a[5]).toFile()),ClassFileLocator.ForJarFile.of(Path.of(a[6]).toFile()),ClassFileLocator.ForFolder.of(Path.of(a[3]).toFile(),net.bytebuddy.ClassFileVersion.ofThisVm()),ClassFileLocator.ForClassLoader.of(CompanionCompositionContract.class.getClassLoader()))){
   registry.addAll(annotations(names(Path.of(a[2])),locator));
   for(int index:new int[]{5,6}){
    var patches=annotations(names(Path.of(a[index])),locator);int unique=(int)patches.stream().map(Patch::advice).distinct().count();if(index==5)lightAdvice=unique;else weatherAdvice=unique;
    for(var patch:patches){byte[] original=locator.locate(patch.owner).resolve();byte[] changed=SurfaceCompositionContract.apply(patch.owner,patch.method,patch.descriptor,patch.advice,original,locator.locate(patch.advice).resolve(),locator);if(changed.length<=original.length)throw new AssertionError("Missing current companion advice "+patch);lastChecks++;}
    registry.addAll(patches);
   }
   for(var patch:registry)ownTargets.add(patch.owner+"."+patch.method);var own=new ArrayList<String>();try(var paths=Files.walk(Path.of(a[3]))){for(var file:paths.filter(p->p.toString().endsWith(".class")).sorted().toList())own.add(Path.of(a[3]).relativize(file).toString().replace('\\','.').replace('/','.').replaceAll("\\.class$",""));}
   registry.addAll(annotations(own,locator));var groups=new LinkedHashMap<String,List<Patch>>();for(var patch:registry)groups.computeIfAbsent(patch.target(),k->new ArrayList<>()).add(patch);
   for(var group:groups.values()){
    if(group.size()<2||group.stream().noneMatch(p->p.advice.startsWith("vpinteriors.")||p.advice.startsWith("vpheadlights.")||p.advice.startsWith("vptw.")))continue;
    var known=new HashSet<String>();for(var patch:group)known.add(patch.advice);if(covered(known))continue;sharedTargets++;
    for(boolean reverse:new boolean[]{false,true}){var order=new ArrayList<>(group);if(reverse)Collections.reverse(order);var first=order.getFirst();byte[] current=locator.locate(first.owner).resolve();for(var patch:order)current=SurfaceCompositionContract.apply(patch.owner,patch.method,patch.descriptor,patch.advice,current,locator.locate(patch.advice).resolve(),locator);if(current.length==0)throw new AssertionError("Empty shared target");lastChecks++;}
   }
  }
  Files.write(output.resolve("companion-targets.txt"),registry.stream().map(p->p.advice+" -> "+p.target()).toList());
  System.out.println("Current light/weather and shared composition: "+lastChecks+" cold checks passed");
 }
}
