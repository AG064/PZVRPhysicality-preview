package pzvrphysicality;

/** Measured region labels for one original sash family. This alone does not authorize native suppression. */
public record WindowMotionProfile(boolean north,String closed,String open,float left,float right,float lower,float glassLower,float middle,float upper,float travel) {
 public static WindowMotionProfile forSprite(String sprite){
  if("fixtures_windows_01_0".equals(sprite)||"fixtures_windows_01_2".equals(sprite))return new WindowMotionProfile(false,"fixtures_windows_01_0","fixtures_windows_01_2",-13/64f,16/64f,64/64f,67/64f,90/64f,117/64f,22/64f);
  if("fixtures_windows_01_1".equals(sprite)||"fixtures_windows_01_3".equals(sprite))return new WindowMotionProfile(true,"fixtures_windows_01_1","fixtures_windows_01_3",-13/64f,16/64f,65/64f,68/64f,91/64f,118/64f,21/64f);
  return null;
 }
 public WindowMotionProfile {
  if(closed==null||open==null||left>=right||lower>=glassLower||glassLower>=middle||middle>=upper||travel<=0||travel>upper-middle)throw new IllegalArgumentException("Measured window region order");
  for(float value:new float[]{left,right,lower,glassLower,middle,upper,travel})if(!Float.isFinite(value)||Math.abs(value)>4)throw new IllegalArgumentException("Measured window region bounds");
 }
 public enum Region {FRAME,UPPER,LOWER,CLEAR}
 /** Surface rows are Y-up. Upper art incorporates the entering sash and is sampled once. */
 public Region region(double along,double height,double progress){
  validate(progress);if(!Double.isFinite(along)||!Double.isFinite(height))throw new IllegalArgumentException("Window sample");
  if(along<left||along>right||height<lower||height>upper)return Region.FRAME;
  if(height>=middle)return Region.UPPER;
  double source=height-travel*progress;return source>=lower&&source<middle?Region.LOWER:Region.CLEAR;
 }
 public double sourceHeight(double height,double progress){validate(progress);return height-travel*progress;}
 public double materialProgress(double height,double progress){validate(progress);if(!Double.isFinite(height))throw new IllegalArgumentException("Window material height");if(progress==0||progress==1)return progress;if(height<middle)return 0;double coverage=Math.max(0,Math.min(1,(middle+3/64d+travel*progress-height)*64));return progress*coverage;}
 /** Single RGBA interpolation, including alpha. No second baked sash layer is composited. */
 public static float[] material(float[] closed,float[] open,double progress){
  validate(progress);if(closed==null||open==null||closed.length!=4||open.length!=4)throw new IllegalArgumentException("Window RGBA");var out=new float[4];
  for(int n=0;n<4;n++){if(!Float.isFinite(closed[n])||!Float.isFinite(open[n])||closed[n]<0||closed[n]>1||open[n]<0||open[n]>1)throw new IllegalArgumentException("Window RGBA bounds");out[n]=(float)(closed[n]*(1-progress)+open[n]*progress);}return out;
 }
 static void validate(double progress){if(!Double.isFinite(progress)||progress<0||progress>1)throw new IllegalArgumentException("Window progress");}
}
