package local.pzvr;
/** The audited upstream presence owner and method are package-private. */
public final class PresenceFixture {
 public static Object presence(boolean value){return new AuthoredPresence(value);}
 private PresenceFixture(){}
}
final class AuthoredPresence {private final boolean allowed;AuthoredPresence(boolean allowed){this.allowed=allowed;}boolean allowsInput(){return allowed;}}
