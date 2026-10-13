package pzvrphysicality;

import java.lang.reflect.Field;import java.util.*;import org.joml.Matrix4f;import viewpoint.core.Frame;import viewpoint.render.ModelDraws;import zombie.core.skinnedmodel.model.ModelInstance;

/** Copies only current local-character attachment positions from original model packets. */
public final class BodyAnchorCapture {
 private record View(Object actor,Object scene,long generation,long time,Map<String,ContactFrame.Point> points){}
 private static volatile View view;private static Field instances,values;
 private static String key(String value){return value==null?"":value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
 public static void clear(){view=null;}
 public static void capture(Frame frame){if(!NativeBody.enabled()){clear();return;}long now=System.nanoTime();var rig=WorldRigCapture.forScene(frame.scene,now);if(rig==null){clear();return;}try{if(instances==null){instances=ModelDraws.class.getDeclaredField("instances");instances.setAccessible(true);values=ModelDraws.class.getDeclaredField("values");values.setAccessible(true);}var records=(ModelInstance[])instances.get(frame.scene.models);var rows=(float[])values.get(frame.scene.models);int count=frame.scene.models.count();if(count<0||count>4096||count>records.length||count>rows.length/68)throw new IllegalArgumentException("Body packet extent");var points=new HashMap<String,ContactFrame.Point>();var ambiguous=new HashSet<String>();for(int n=0;n<count;n++){var instance=records[n];if(instance==null||instance.attachmentNameParent==null)continue;boolean local=false;var parent=instance;for(int depth=0;parent!=null&&depth<32;depth++,parent=parent.parent)if(parent.character==rig.actor()){local=true;break;}if(!local)continue;String name=key(instance.attachmentNameParent);if(name.isEmpty())continue;if(points.containsKey(name)){ambiguous.add(name);continue;}if(points.size()>=24)continue;var m=new Matrix4f().set(rows,n*68);if(!m.isFinite())continue;var p=WorldRigFrame.fromRender(frame.camX,frame.camY,frame.camZ,new MouthMath.Vec(m.m30(),m.m31(),m.m32()));points.put(name,p);}for(var name:ambiguous)points.remove(name);view=new View(rig.actor(),rig.scene(),rig.generation(),now,Map.copyOf(points));}catch(ReflectiveOperationException|RuntimeException unsupported){clear();}}
 public static ContactFrame.Point point(WorldRigFrame rig,String location){var v=view;long now=System.nanoTime();return v!=null&&rig!=null&&v.actor==rig.actor()&&v.scene==rig.scene()&&v.generation==rig.generation()&&now>=v.time&&now-v.time<150_000_000L?v.points.get(key(location)):null;}
 private BodyAnchorCapture(){}
}
