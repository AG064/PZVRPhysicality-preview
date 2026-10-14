package zombie.characters;
public class IsoGameCharacter {
 public boolean dead;public zombie.inventory.InventoryItem left,right;public final java.util.Stack<zombie.characters.CharacterTimedActions.BaseAction> actions=new java.util.Stack<>();
 public boolean isDead(){return dead;}public zombie.inventory.InventoryItem getPrimaryHandItem(){return right;}public zombie.inventory.InventoryItem getSecondaryHandItem(){return left;}
 public java.util.Stack<zombie.characters.CharacterTimedActions.BaseAction> getCharacterActions(){return actions;}
}
