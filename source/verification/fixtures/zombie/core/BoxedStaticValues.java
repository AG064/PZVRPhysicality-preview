package zombie.core;
/** Numeric boxing for the authored Lua runtime environment. */
public final class BoxedStaticValues {
 private BoxedStaticValues(){}
 public static Double toDouble(double value){return Double.valueOf(value);}
}
