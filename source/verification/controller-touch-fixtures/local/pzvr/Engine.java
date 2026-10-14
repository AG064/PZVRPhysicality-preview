package local.pzvr;
/** Authored input publication and eligibility only. */
public final class Engine {
 public static volatile XrBackend.Frame latestInput,latest;public static boolean enabled,worldPresent;public static Settings settings;
 public static boolean active(){return enabled;}
 private Engine(){}
}
