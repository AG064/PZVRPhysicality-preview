package pzvrphysicality;

import java.util.List;

/** Exact native icon/model pairs only; an unknown or ambiguous colour keeps the native result. */
public final class IndexedModelChoice {
 public static String choose(String texture,List<String> icons,List<String> models){
  if(texture==null||icons==null||models==null||icons.isEmpty()||icons.size()>32||icons.size()!=models.size())return null;
  String name=texture.replace('\\','/');int slash=name.lastIndexOf('/');if(slash>=0)name=name.substring(slash+1);
  if(name.endsWith(".png"))name=name.substring(0,name.length()-4);if(name.startsWith("Item_"))name=name.substring(5);
  String found=null;int matches=0;
  for(int n=0;n<icons.size();n++)if(name.equals(icons.get(n))){String model=models.get(n);if(model==null||model.isBlank()||model.length()>160||model.chars().anyMatch(Character::isISOControl))return null;found=model;matches++;}
  return matches==1?found:null;
 }
 private IndexedModelChoice(){}
}
