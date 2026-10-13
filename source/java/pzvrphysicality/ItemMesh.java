package pzvrphysicality;

import viewpointvr.physics.CollisionShape;

/** Numeric conversion for the next item-geometry adapter. Never holds a native mesh or frame reference. */
public final class ItemMesh {
 /** Column-major native model matrix, native render XYZ, world XY/height and the body's normalized pose. */
 public static CollisionShape fromRendered(float[] vertices,int[] faces,float[] matrix,double camX,double camY,double camFloor,float[] body) {
  if(vertices==null||vertices.length<12||vertices.length>96000||vertices.length%3!=0||faces==null||faces.length<3||faces.length>192000||faces.length%3!=0
    ||matrix==null||matrix.length!=16||body==null||body.length<7||!Double.isFinite(camX)||!Double.isFinite(camY)||!Double.isFinite(camFloor))throw new IllegalArgumentException("Item mesh extent");
  for(float v:matrix)if(!Float.isFinite(v))throw new IllegalArgumentException("Model matrix");
  for(int i=0;i<7;i++)if(!Float.isFinite(body[i]))throw new IllegalArgumentException("Body pose");
  double norm=body[3]*body[3]+body[4]*body[4]+body[5]*body[5]+body[6]*body[6];
  if(Math.abs(norm-1)>.002||Math.abs(matrix[3])+Math.abs(matrix[7])+Math.abs(matrix[11])+Math.abs(matrix[15]-1)>.0001)throw new IllegalArgumentException("Item affine/rotation convention");
  for(int face:faces)if(face<0||face>=vertices.length/3)throw new IllegalArgumentException("Item triangle");
  var local=new float[vertices.length];double qx=-body[3],qy=-body[4],qz=-body[5],qw=body[6];
  for(int i=0;i<vertices.length;i+=3){
   double x=vertices[i],y=vertices[i+1],z=vertices[i+2];if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Item vertex");
   double rx=matrix[0]*x+matrix[4]*y+matrix[8]*z+matrix[12];
   double ry=matrix[1]*x+matrix[5]*y+matrix[9]*z+matrix[13];
   double rz=matrix[2]*x+matrix[6]*y+matrix[10]*z+matrix[14];
   double wx=camX-rx-body[0],wy=camY-rz-body[1],wh=camFloor*Math.sqrt(6)+ry-body[2];
   double tx=2*(qy*wh-qz*wy),ty=2*(qz*wx-qx*wh),tz=2*(qx*wy-qy*wx);
   local[i]=(float)(wx+qw*tx+qy*tz-qz*ty);local[i+1]=(float)(wy+qw*ty+qz*tx-qx*tz);local[i+2]=(float)(wh+qw*tz+qx*ty-qy*tx);
  }
  // Model-render to world space reverses handedness. Preserve triangle orientation explicitly.
  var triangles=faces.clone();for(int i=0;i<triangles.length;i+=3){int swap=triangles[i+1];triangles[i+1]=triangles[i+2];triangles[i+2]=swap;}
  return new CollisionShape(local,triangles,new float[0],true);
 }
 private ItemMesh(){}
}
