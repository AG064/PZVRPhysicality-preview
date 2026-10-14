package pzvrphysicality;

import zombie.inventory.InventoryItem;
import zombie.scripting.ScriptManager;

/** Repairs an unset indexed model without changing native inventory, colour, food state or attachments. */
public final class ItemModelFallback {
 private static volatile boolean enabled;private static volatile long resolved,rejected,generation;
 public static void configure(boolean value){if(value&&!Main.ready())throw new IllegalStateException("SDK validation pending");enabled=value;generation++;resolved=rejected=0;}
 public static boolean enabled(){return enabled;}
 public static String status(){return enabled?"Exact native indexed models armed; resolved "+resolved+", unmatched "+rejected:"Indexed model repair Off";}
 public static String resolve(InventoryItem item,String original,boolean world){
  if(original!=null||!enabled||!Main.ready()||item==null||item.getModelIndex()!=-1)return original;
  long mode=generation;
  try{
   if(item.hasModData()){
    var data=item.getModData();if(data.rawget(world?"worldStaticModel":"staticModel")!=null||data.rawget("customInventoryIcon")!=null)return original;
   }
   var texture=item.getIcon();var manager=ScriptManager.instance;var icons=item.getIconsForTexture();var models=world?item.getWorldStaticModelsByIndex():item.getStaticModelsByIndex();
   String model=IndexedModelChoice.choose(texture==null?null:texture.getName(),icons,models);
   if(model==null||manager==null||manager.getModelScript(model)==null){rejected++;return original;}
   if(!enabled||mode!=generation||!Main.ready()||item.getModelIndex()!=-1||item.getIcon()!=texture||item.getIconsForTexture()!=icons||(world?item.getWorldStaticModelsByIndex():item.getStaticModelsByIndex())!=models||!model.equals(IndexedModelChoice.choose(texture.getName(),icons,models)))return original;
   resolved++;return model;
  }catch(RuntimeException|LinkageError unsupported){rejected++;return original;}
 }
 private ItemModelFallback(){}
}
