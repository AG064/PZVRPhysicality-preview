package pzvrphysicality;
/** Legacy consumer boundary: exact provider availability is explicit; no SDK owner verification is bypassed. */
public final class SdkPins {public static boolean shelves=true;public static boolean shelvesAvailable(){return shelves;}public static void verify(Class<?> type,String expected){throw new AssertionError("Unexpected SDK owner verification in authored consumer");}public static void verifyRuntime(){throw new AssertionError("Original SDK startup forbidden in authored consumer");}}
