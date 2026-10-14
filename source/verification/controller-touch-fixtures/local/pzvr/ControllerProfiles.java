package local.pzvr;
/** Authored record API, contains no upstream profile implementation. */
public final class ControllerProfiles {
 public record Binding(String action,String path){}
 public record Profile(String id,String name,String extension,java.util.List<Binding> bindings){}
 private ControllerProfiles(){}
}
