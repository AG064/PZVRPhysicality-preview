package pzvrphysicality;

import viewpointvr.physics.CollisionShape;
import viewpointvr.physics.PackedModelGeometry;

/** Uses Viewpoint's native bind transform once, with exact triangles and no solid bounding proxy. */
public final class MeshPlacement {
 public static CollisionShape place(PackedModelGeometry.Mesh mesh,float cos,float sin,float scale) {
  if(mesh==null||!Float.isFinite(cos)||!Float.isFinite(sin)||Math.abs(cos*cos+sin*sin-1)>.002
    ||!Float.isFinite(scale)||scale<=0||scale>16)throw new IllegalArgumentException("Packed transform");
  var vertices=mesh.vertices();
  for(int i=0;i<vertices.length;i+=3){float x=vertices[i]*scale,y=vertices[i+1]*scale,z=vertices[i+2]*scale;
   vertices[i]=-x*cos+z*sin;vertices[i+1]=-x*sin-z*cos;vertices[i+2]=y;
  }
  var faces=mesh.faces();for(int i=0;i<faces.length;i+=3){int swap=faces[i+1];faces[i+1]=faces[i+2];faces[i+2]=swap;}
  return new CollisionShape(vertices,faces,new float[0],true,true);
 }
 private MeshPlacement(){}
}
