package viewpointvr.physics;

/** Copied collision geometry. It holds no native mesh, item, drawer or GPU references. */
public record CollisionShape(float[] vertices,int[] faces,float[] boxes,boolean exact,boolean staticOnly) {
 public CollisionShape(float[] vertices,int[] faces,float[] boxes,boolean exact){this(vertices,faces,boxes,exact,false);}
 public CollisionShape {
  vertices=vertices.clone();faces=faces.clone();boxes=boxes.clone();
  if(vertices.length>96000||faces.length>192000||boxes.length>640||(vertices.length==0)==(boxes.length==0))throw new IllegalArgumentException("Collision shape budget");
  for(float value:vertices)if(!Float.isFinite(value)||Math.abs(value)>(staticOnly?16:4))throw new IllegalArgumentException("Collision vertex");
  for(float value:boxes)if(!Float.isFinite(value)||Math.abs(value)>32)throw new IllegalArgumentException("Collision box");
 }
 public float[] vertices(){return vertices.clone();}
 public int[] faces(){return faces.clone();}
 public float[] boxes(){return boxes.clone();}
 public boolean sameGeometry(CollisionShape other){return other!=null&&exact==other.exact&&staticOnly==other.staticOnly&&java.util.Arrays.equals(faces,other.faces)&&near(vertices,other.vertices)&&near(boxes,other.boxes);}
 public static boolean near(float[] first,float[] second){if(first.length!=second.length)return false;for(int i=0;i<first.length;i++)if(!Float.isFinite(first[i])||!Float.isFinite(second[i])||Math.abs(first[i]-second[i])>.0001f)return false;return true;}
 public long body(RigidPhysics world,int kind,float[] pose,float mass){if(staticOnly&&kind!=0)throw new IllegalArgumentException("Static contact mesh cannot become a held body");return boxes.length>0?world.boxes(kind,pose,mass,boxes):world.mesh(kind,pose,mass,vertices,faces);}
 public static CollisionShape box(float x,float y,float z,float cx,float cy,float cz){return new CollisionShape(new float[0],new int[0],RigidPhysics.box(x,y,z,cx,cy,cz),false);}
}
