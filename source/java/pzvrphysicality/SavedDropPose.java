package pzvrphysicality;

/** Validated detached persisted quaternion and its native-placement fingerprint. */
public record SavedDropPose(int id,int x,int y,int z,float ox,float oy,float oz,float dh,float qx,float qy,float qz,float qw) {
 public SavedDropPose {
  if(x<0||x>100000||y<0||y>100000||z< -32||z>32)throw new IllegalArgumentException("Saved item square");
  for(float value:new float[]{ox,oy,oz,dh,qx,qy,qz,qw})if(!Float.isFinite(value))throw new IllegalArgumentException("Saved item number");
  if(ox<0||ox>=1||oy<0||oy>=1||oz<0||oz>2||Math.abs(dh)>.3)throw new IllegalArgumentException("Saved item offset");
  double norm=qx*qx+qy*qy+qz*qz+qw*qw;
  if(norm<.98||norm>1.02)throw new IllegalArgumentException("Saved item quaternion");
  float scale=(float)(1/Math.sqrt(norm));qx*=scale;qy*=scale;qz*=scale;qw*=scale;
 }
 public boolean matches(int itemId,int sx,int sy,int sz,float offX,float offY,float offZ){
  return itemId==id&&sx==x&&sy==y&&sz==z&&near(ox,offX)&&near(oy,offY)&&near(oz,offZ);
 }
 private static boolean near(float a,float b){return Float.isFinite(b)&&Math.abs(a-b)<.0001f;}
 public float[] pose(){return new float[]{x+ox,y+oy,(float)((z+oz)*Math.sqrt(6))+dh,qx,qy,qz,qw};}
}
