package local.pzvr;
/** Pinned descriptor-compatible value fixtures. No OpenXR or backend lifecycle exists here. */
public interface XrBackend {
 int TRIGGER=1,GRIP=2,PRIMARY=4,SECONDARY=8,STICK=16,MENU=32,ROLE_PRIMARY=64,ROLE_SECONDARY=128,OTHER_PRIMARY=256,OTHER_SECONDARY=512,OTHER_MENU=1024,ROLE_MENU=2048,DPAD_UP=4096,DPAD_DOWN=8192,DPAD_LEFT=16384,DPAD_RIGHT=32768,BUMPER=65536;
 record Fov(float left,float right,float up,float down){}
 record Eye(Math3.Pose pose,Fov fov,int width,int height){}
 record Hand(Math3.Pose grip,Math3.Pose aim,boolean tracked,float stickX,float stickY,int buttons){
  public static final Hand EMPTY=new Hand(null,null,false,0,0,0);
  public Hand(Object present,boolean tracked,boolean pressed){this(present==null?null:Math3.Pose.IDENTITY,null,tracked,0,0,pressed?GRIP:0);}
  public boolean down(int bit){return (buttons&bit)!=0;}
 }
 record Frame(long time,boolean render,boolean focused,Math3.Pose head,java.util.List<Eye> eyes,Hand left,Hand right){public Frame(long time,boolean focused,Hand left,Hand right){this(time,false,focused,null,java.util.List.of(),left,right);}}
}
