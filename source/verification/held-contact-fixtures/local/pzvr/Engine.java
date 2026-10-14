package local.pzvr;
public final class Engine {public static volatile XrBackend.Frame latestInput;public static boolean enabled,worldPresent;public static Settings settings=new Settings();public static volatile int hapticRequest=-1;public static boolean active(){return enabled;}}
