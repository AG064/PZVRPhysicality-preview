package pzvrphysicality;
import zombie.characters.IsoPlayer;import zombie.inventory.InventoryItem;

/** Read-only logical inventory slots. A model reference does not prove it was drawn. */
public final class NativeHandState {
 private static String type(InventoryItem item){if(item==null)return "empty";String value=item.getFullType();if(value==null)return "unknown native type";return value.length()<=160?value:value.substring(0,157)+"...";}
 private static String slot(String name,InventoryItem item,boolean model,boolean busy){return name+"="+type(item)+" native-model-reference="+(model?"present":"none")+" physical-gesture-busy="+busy;}
 public static String status(boolean leftBusy,boolean rightBusy){
  try{
   var players=IsoPlayer.players;var player=players==null||players.length==0?null:players[0];if(player==null)return "Logical native hand slots: no local actor";
   var left=player.getSecondaryHandItem();var right=player.getPrimaryHandItem();var leftModel=player.secondaryHandModel;var rightModel=player.primaryHandModel;
   if(IsoPlayer.players!=players||players[0]!=player||player.getSecondaryHandItem()!=left||player.getPrimaryHandItem()!=right||player.secondaryHandModel!=leftModel||player.primaryHandModel!=rightModel)return "Logical native hand slots changed during read; retry";
   return "Logical slots; "+slot("Left/secondary",left,leftModel!=null,leftBusy)+"; "+slot("Right/primary",right,rightModel!=null,rightBusy);
  }catch(RuntimeException|LinkageError unavailable){return "Native hand slots unavailable: "+unavailable.getClass().getSimpleName();}
 }
 private NativeHandState(){}
}
