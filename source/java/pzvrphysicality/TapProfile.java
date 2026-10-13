package pzvrphysicality;

import java.util.List;

/** Functional anchor measurements from the selected original models; no copied model or texture payloads. */
public record TapProfile(String model,String sha256,float[] bounds,List<MouthMath.Vec> handles,MouthMath.Vec spout){
 public TapProfile{bounds=bounds.clone();handles=List.copyOf(handles);if(model==null||sha256==null||!sha256.matches("[0-9a-f]{64}")||bounds.length!=6||handles.isEmpty()||handles.size()>4||spout==null)throw new IllegalArgumentException("Tap profile");for(float value:bounds)if(!Float.isFinite(value))throw new IllegalArgumentException("Tap bounds");}
 public float[] bounds(){return bounds.clone();}
 public boolean matches(float[] actual){if(actual==null||actual.length!=6)return false;for(int i=0;i<6;i++)if(!Float.isFinite(actual[i])||Math.abs(actual[i]-bounds[i])>.00015f)return false;return true;}
 public ContactFrame.Point placed(MouthMath.Vec point,ContactFrame.Point base,float c,float s,float scale){
  if(point==null||base==null||!Float.isFinite(c)||!Float.isFinite(s)||Math.abs(c*c+s*s-1)>.002||!Float.isFinite(scale)||scale<=0||scale>4)throw new IllegalArgumentException("Tap placement");
  double x=point.x()*scale,z=point.z()*scale;return new ContactFrame.Point(base.x()-x*c+z*s,base.y()-x*s-z*c,base.height()+point.y()*scale);
 }
 private static TapProfile profile(String name,String hash){return new TapProfile(name,hash,new float[]{-.263000011f,.67698431f,-.402841985f,.263000011f,1.10660696f,.0095f},
  List.of(new MouthMath.Vec(.134999644,.950923622,-.3799945415),new MouthMath.Vec(-.1349947115,.9509238605,-.3799967765)),new MouthMath.Vec(-.0000001245,1.0378059745,-.2661809175));}
 private static final List<TapProfile> PROFILES=List.of(
  profile("pz_sink_inset_beige.obj","6c090e4bf4cdf325c55d55476d012fda250d6520a93aac8b680b4c80f1cb5ff7"),
  profile("pz_sink_inset_white.obj","34c6f33e0b863e271be313fe1f655f104d3a4a8a8f9b91da418ff13927e83e92"));
 public static TapProfile find(String name){return PROFILES.stream().filter(p->p.model.equals(name)).findFirst().orElse(null);}
}
