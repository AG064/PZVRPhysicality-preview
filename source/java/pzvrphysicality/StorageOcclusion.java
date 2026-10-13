package pzvrphysicality;

/** Bounded, two-sided segment tests against the original furniture triangles. No solid-box replacement. */
public final class StorageOcclusion {
 private final float[] vertices;private final int[] faces;
 public StorageOcclusion(float[] vertices,int[] faces){
  if(vertices==null||vertices.length<9||vertices.length>96000||vertices.length%3!=0||faces==null||faces.length<3||faces.length>60000||faces.length%3!=0)throw new IllegalArgumentException("Storage reach geometry budget");
  for(float v:vertices)if(!Float.isFinite(v)||Math.abs(v)>16)throw new IllegalArgumentException("Storage reach vertex");
  for(int f:faces)if(f<0||f>=vertices.length/3)throw new IllegalArgumentException("Storage reach index");this.vertices=vertices.clone();this.faces=faces.clone();
 }
 public boolean blocked(ContactFrame.Point start,ContactFrame.Point end){
  double dx=end.x()-start.x(),dy=end.height()-start.height(),dz=end.y()-start.y();
  if(dx*dx+dy*dy+dz*dz<1e-12)return false;
  for(int n=0;n<faces.length;n+=3){
   int a=faces[n]*3,b=faces[n+1]*3,c=faces[n+2]*3;
   double ex=vertices[b]-vertices[a],ey=vertices[b+1]-vertices[a+1],ez=vertices[b+2]-vertices[a+2];
   double fx=vertices[c]-vertices[a],fy=vertices[c+1]-vertices[a+1],fz=vertices[c+2]-vertices[a+2];
   double px=dy*fz-dz*fy,py=dz*fx-dx*fz,pz=dx*fy-dy*fx,det=ex*px+ey*py+ez*pz;
   if(Math.abs(det)<1e-10)continue;
   double tx=start.x()-vertices[a],ty=start.height()-vertices[a+1],tz=start.y()-vertices[a+2];
   double u=(tx*px+ty*py+tz*pz)/det;if(u< -1e-7||u>1+1e-7)continue;
   double qx=ty*ez-tz*ey,qy=tz*ex-tx*ez,qz=tx*ey-ty*ex;
   double v=(dx*qx+dy*qy+dz*qz)/det;if(v< -1e-7||u+v>1+1e-7)continue;
   double t=(fx*qx+fy*qy+fz*qz)/det;if(t>.0001&&t<.9999)return true;
  }return false;
 }
 public boolean aperture(StorageFrame frame,ContactFrame.Point local){
  return !blocked(new ContactFrame.Point(local.x(),frame.minD()-.05/frame.scale(),local.height()),local)
   ||!blocked(new ContactFrame.Point(local.x(),frame.maxD()+.05/frame.scale(),local.height()),local);
 }
 public double support(double x,double depth,double below){
  double result=Double.NaN;
  for(int n=0;n<faces.length;n+=3){
   int a=faces[n]*3,b=faces[n+1]*3,c=faces[n+2]*3;
   double ax=vertices[a],az=vertices[a+2],bx=vertices[b],bz=vertices[b+2],cx=vertices[c],cz=vertices[c+2];
   double det=(bz-cz)*(ax-cx)+(cx-bx)*(az-cz);if(Math.abs(det)<1e-10)continue;
   double u=((bz-cz)*(x-cx)+(cx-bx)*(depth-cz))/det,v=((cz-az)*(x-cx)+(ax-cx)*(depth-cz))/det;
   if(u< -1e-7||v< -1e-7||u+v>1+1e-7)continue;
   double ex=bx-ax,ey=vertices[b+1]-vertices[a+1],ez=bz-az,fx=cx-ax,fy=vertices[c+1]-vertices[a+1],fz=cz-az;
   double nx=ey*fz-ez*fy,ny=ez*fx-ex*fz,nz=ex*fy-ey*fx;
   if(ny*ny<.9*(nx*nx+ny*ny+nz*nz))continue;
   double height=u*vertices[a+1]+v*vertices[b+1]+(1-u-v)*vertices[c+1];
   if(height<=below&&(!Double.isFinite(result)||height>result))result=height;
  }return result;
 }
}
