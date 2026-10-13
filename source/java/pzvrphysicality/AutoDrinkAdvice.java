package pzvrphysicality;
import me.zed_0xff.zombie_buddy.Patch;
/** Preserve the user's saved autoDrink preference; lease suppression only in the physical VR mode. */
@Patch(className="zombie.characters.IsoGameCharacter",methodName="autoDrink",strictMatch=true)
public final class AutoDrinkAdvice {
 @Patch.OnEnter(skipOn=true) public static boolean enter(@Patch.This Object player){return NativeConsumption.suppressAutoDrink(player);}
 private AutoDrinkAdvice(){}
}
