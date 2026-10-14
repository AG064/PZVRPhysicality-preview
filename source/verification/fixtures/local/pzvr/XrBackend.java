package local.pzvr;
public interface XrBackend {
 int TRIGGER=1,GRIP=2;
 record Eye(){}
 record Hand(Math3.Pose grip,Math3.Pose aim,boolean tracked,float stickX,float stickY,int buttons){public boolean down(int mask){return (buttons&mask)!=0;}}
 record Frame(long time,boolean render,boolean focused,Math3.Pose head,java.util.List<Eye> eyes,Hand left,Hand right){}
}
