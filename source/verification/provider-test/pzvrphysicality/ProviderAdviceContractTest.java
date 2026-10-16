package pzvrphysicality;
import java.nio.file.*;import java.util.*;import net.bytebuddy.ByteBuddy;import net.bytebuddy.asm.Advice;import net.bytebuddy.dynamic.ClassFileLocator;import net.bytebuddy.pool.TypePool;import net.bytebuddy.jar.asm.*;import static net.bytebuddy.matcher.ElementMatchers.named;

/** Copies and transforms pinned classes without defining or initializing any original owner. */
public final class ProviderAdviceContractTest {
 private static int checks;private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);checks++;}
 private static byte[] advice(Path classes,String name)throws Exception{
  var reader=new ClassReader(Files.readAllBytes(classes.resolve("pzvrphysicality/"+name+".class")));var writer=new ClassWriter(0);
  var names=Map.of("OnEnter","OnMethodEnter","OnExit","OnMethodExit","Argument","Argument","Return","Return","Thrown","Thrown");
  reader.accept(new ClassVisitor(Opcodes.ASM9,writer){String annotation(String d){String prefix="Lme/zed_0xff/zombie_buddy/Patch$";if(!d.startsWith(prefix))return d;String key=d.substring(prefix.length(),d.length()-1);if(!names.containsKey(key))throw new AssertionError("Unknown annotation "+key);return "Lnet/bytebuddy/asm/Advice$"+names.get(key)+";";}
   public MethodVisitor visitMethod(int access,String n,String d,String s,String[] e){return new MethodVisitor(Opcodes.ASM9,super.visitMethod(access,n,d,s,e)){
    public AnnotationVisitor visitAnnotation(String d,boolean v){return new AnnotationVisitor(Opcodes.ASM9,super.visitAnnotation(annotation(d),v)){public void visit(String n,Object value){if(d.endsWith("Patch$OnEnter;")&&n.equals("skipOn")&&value instanceof Boolean b){if(b)super.visit(n,Type.getType("Lnet/bytebuddy/asm/Advice$OnNonDefaultValue;"));return;}super.visit(n,value);}};}
    public AnnotationVisitor visitParameterAnnotation(int p,String d,boolean v){return super.visitParameterAnnotation(p,annotation(d),v);}
   };}
  },0);return writer.toByteArray();
 }
 private static Map<String,Integer> nativeCalls(byte[] bytes,String method){var calls=new TreeMap<String,Integer>();new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9){public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){return !n.equals(method)?null:new MethodVisitor(Opcodes.ASM9){public void visitMethodInsn(int op,String owner,String n,String d,boolean i){if(owner.startsWith("viewpoint/")||owner.startsWith("zombie/"))calls.merge(owner+"."+n+d,1,Integer::sum);}};}},0);return calls;}
 private static int callbacks(byte[] bytes,String method,String call){int[] count={0};new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9){public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){return !n.equals(method)?null:new MethodVisitor(Opcodes.ASM9){public void visitMethodInsn(int op,String owner,String n,String d,boolean i){if(owner.equals("pzvrphysicality/StorageProviders")&&n.equals(call))count[0]++;}};}},0);return count[0];}
 private static void compose(Path classes,Path baseline,Path output,ClassFileLocator dependencies,String owner,String method,String descriptor,String[] patches,String callback)throws Exception{
  byte[] original=dependencies.locate(owner).resolve(),bytes=original;var pool=TypePool.Default.of(dependencies);var targets=pool.describe(owner).resolve().getDeclaredMethods().filter(named(method));check(targets.size()==1&&targets.getOnly().getDescriptor().equals(descriptor),"exact pinned "+method+" descriptor");
  for(String patch:patches){Path source=Files.isRegularFile(classes.resolve("pzvrphysicality/"+patch+".class"))?classes:baseline;byte[] mapped=advice(source,patch);String name="pzvrphysicality."+patch;var locator=new ClassFileLocator.Compound(new ClassFileLocator.Simple(Map.of(owner,bytes,name,mapped)),dependencies);var types=TypePool.Default.of(locator);bytes=new ByteBuddy().redefine(types.describe(owner).resolve(),locator).visit(Advice.to(types.describe(name).resolve(),locator).on(named(method))).make().getBytes();}
  check(nativeCalls(original,method).equals(nativeCalls(bytes,method)),"all original native invocations survive "+Arrays.toString(patches));check(callbacks(bytes,method,callback)==1,"exactly one provider callback at "+method);Files.write(output.resolve(method+"-"+String.join("-",patches)+".class"),bytes);
 }
 public static void main(String[] args)throws Exception{
  Path classes=Path.of(args[0]),baseline=Path.of(args[1]),out=Path.of(args[2]);Files.createDirectories(out);
  try(var dependencies=new ClassFileLocator.Compound(ClassFileLocator.ForJarFile.of(Path.of(args[3]).toFile()),ClassFileLocator.ForJarFile.of(Path.of(args[4]).toFile()),ClassFileLocator.ForJarFile.of(Path.of(args[5]).toFile()),new ClassFileLocator.ForFolder(classes.toFile()),new ClassFileLocator.ForFolder(baseline.toFile()),ClassFileLocator.ForClassLoader.ofSystemLoader())){
   for(String[] order:new String[][]{{"ProviderPackObjectAdvice","CounterTapObjectAdvice"},{"CounterTapObjectAdvice","ProviderPackObjectAdvice"}})compose(classes,baseline,out,dependencies,"viewpoint.world.PackGather","object","(Lzombie/iso/IsoObject;Lzombie/iso/sprite/IsoSprite;FFFI)Z",order,"packBegin");
   compose(classes,baseline,out,dependencies,"viewpoint.world.PackGather","part","(Lzombie/iso/sprite/IsoSprite;FFFI)Z",new String[]{"ProviderPackPartAdvice"},"packPart");
   String[] names={"ProviderPackRecipeAdvice","CounterTapRecipeAdvice","ShelfRecipeAdvice"};for(int[] p:new int[][]{{0,1,2},{0,2,1},{1,0,2},{1,2,0},{2,0,1},{2,1,0}})compose(classes,baseline,out,dependencies,"viewpoint.world.Recipe","model","(Lviewpoint/world/Recipe;Lviewpoint/packs/PackBind;ZFFFIII)V",new String[]{names[p[0]],names[p[1]],names[p[2]]},"packEmitted");
  }
  System.out.println("Provider hook composition: "+checks+" checks passed");
 }
}
