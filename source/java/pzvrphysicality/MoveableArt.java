package pzvrphysicality;
import java.lang.reflect.*;import java.util.Arrays;import viewpoint.render.PackModel;import static org.lwjgl.opengl.GL33.*;
/** Frame-local borrowed native pack atlas identity and the original atlas rectangle; never owns a texture. */
public final class MoveableArt {
 public record Art(int texture,int page,int x,int y,int width,int height,int storedWidth,int storedHeight){public float[] values(){return new float[]{x/2048f,y/2048f,width/2048f,height/2048f,(x+storedWidth)/2048f,(y+storedHeight)/2048f};}}
 private static Field[] fields;private static Method texture;
 private static int[] metadata(PackModel model)throws ReflectiveOperationException{if(fields==null){String[] names={"artPage","artX","artY","artWidth","artHeight","artW","artH"};var found=new Field[names.length];for(int n=0;n<found.length;n++){found[n]=PackModel.class.getDeclaredField(names[n]);found[n].setAccessible(true);}var type=Class.forName("viewpoint.render.PackArt",false,MoveableArt.class.getClassLoader());var method=type.getDeclaredMethod("texture",int.class);method.setAccessible(true);texture=method;fields=found;}var values=new int[fields.length];for(int i=0;i<values.length;i++)values[i]=fields[i].getInt(model);return values;}
 public static Art read(PackModel model){if(model==null||model.state()!=PackModel.RESIDENT)return null;try{var m=metadata(model);if(m[0]<0||m[1]<0||m[2]<0||m[3]<1||m[4]<1||m[5]<m[3]||m[6]<m[4]||(long)m[1]+m[5]>2048||(long)m[2]+m[6]>2048)return null;int id=(int)texture.invoke(null,m[0]);if(id<=0||!glIsTexture(id)||model.state()!=PackModel.RESIDENT||!Arrays.equals(m,metadata(model))||(int)texture.invoke(null,m[0])!=id)return null;return new Art(id,m[0],m[1],m[2],m[3],m[4],m[5],m[6]);}catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){return null;}}
 public static boolean current(PackModel model,Art expected){return expected!=null&&expected.equals(read(model));}
 /** Numeric resource identity check for the game thread; GL validity is checked only in the render scope. */
 public static boolean metadataCurrent(PackModel model,Art expected){if(model==null||expected==null||model.state()!=PackModel.RESIDENT)return false;try{var m=metadata(model);return expected.page==m[0]&&expected.x==m[1]&&expected.y==m[2]&&expected.width==m[3]&&expected.height==m[4]&&expected.storedWidth==m[5]&&expected.storedHeight==m[6]&&(int)texture.invoke(null,m[0])==expected.texture&&model.state()==PackModel.RESIDENT;}catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){return false;}}
 private MoveableArt(){}
}
