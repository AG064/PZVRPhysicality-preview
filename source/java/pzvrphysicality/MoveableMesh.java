package pzvrphysicality;

import viewpoint.packs.PackBind;
import viewpointvr.physics.CollisionShape;
import viewpointvr.physics.PackedModelGeometry;

/** Detached exact pack triangles, rebased from an authored world placement to a loose-item pivot. */
public final class MoveableMesh {
 public record Fit(PackBind binding,CollisionShape shape){}
 public static Fit fit(PackBind original,PackedModelGeometry.Mesh mesh,float yaw,float worldScale){
  if(original==null||mesh==null||!Float.isFinite(yaw)||!Float.isFinite(worldScale)||worldScale<=0||worldScale>16)throw new IllegalArgumentException("Moveable transform");
  double angle=Math.atan2(original.sin,original.cos)-Math.toRadians(yaw);float scale=original.scale*worldScale;
  if(!Float.isFinite(original.cos)||!Float.isFinite(original.sin)||Math.abs(original.cos*original.cos+original.sin*original.sin-1)>.002||!Float.isFinite(scale)||scale<=0||scale>16)throw new IllegalArgumentException("Moveable binding");
  float c=(float)Math.cos(angle),s=(float)Math.sin(angle);var points=mesh.vertices();var faces=mesh.faces();
  if(points.length<12||points.length>96000||points.length%3!=0||faces.length<3||faces.length>192000||faces.length%3!=0)throw new IllegalArgumentException("Moveable geometry budget");
  float minX=Float.POSITIVE_INFINITY,minY=minX,minH=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX,maxH=maxX;
  for(int i=0;i<points.length;i+=3){float x=points[i]*scale,z=points[i+2]*scale,h=points[i+1]*scale;
   float wx=-x*c+z*s,wy=-x*s-z*c;if(!Float.isFinite(wx)||!Float.isFinite(wy)||!Float.isFinite(h))throw new IllegalArgumentException("Moveable vertex");
   points[i]=wx;points[i+1]=wy;points[i+2]=h;minX=Math.min(minX,wx);maxX=Math.max(maxX,wx);minY=Math.min(minY,wy);maxY=Math.max(maxY,wy);minH=Math.min(minH,h);maxH=Math.max(maxH,h);
  }
  float cx=(minX+maxX)/2,cy=(minY+maxY)/2;
  if(maxX-minX>4||maxY-minY>4||maxH-minH>4||maxX-minX<.001||maxY-minY<.001||maxH-minH<.001)throw new IllegalArgumentException("Moveable physical extent");
  for(int i=0;i<points.length;i+=3){points[i]-=cx;points[i+1]-=cy;points[i+2]-=minH;}
  for(int i=0;i<faces.length;i+=3){for(int j=0;j<3;j++)if(faces[i+j]<0||faces[i+j]>=points.length/3)throw new IllegalArgumentException("Moveable triangle");int swap=faces[i+1];faces[i+1]=faces[i+2];faces[i+2]=swap;}
  return new Fit(new PackBind(original.model,(float)Math.toDegrees(angle),-cx,-cy,-minH,scale),new CollisionShape(points,faces,new float[0],true));
 }
 private MoveableMesh(){}
}
