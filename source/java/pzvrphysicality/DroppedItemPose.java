package pzvrphysicality;

import java.lang.reflect.Field;
import java.util.*;
import org.joml.Matrix4f;import org.joml.Quaternionf;import org.joml.Vector3f;
import viewpoint.core.Frame;
import viewpoint.render.ModelDraws;

/** Applies a numeric physics pose to rigid records from one native world-item producer only. */
public final class DroppedItemPose {
 private static final int STRIDE=68;
 private record Fields(Field values,Field flags,Field palette,Field shadow){}
 private static volatile Fields access;
 public static Matrix4f delta(ContactFrame.Point camera,ContactFrame.Point nativeOrigin,float[] pose){
  if(camera==null||nativeOrigin==null||pose==null||pose.length<7)throw new IllegalArgumentException("Dropped render pose extent");
  for(int i=0;i<7;i++)if(!Float.isFinite(pose[i]))throw new IllegalArgumentException("Dropped render pose");
  double norm=pose[3]*pose[3]+pose[4]*pose[4]+pose[5]*pose[5]+pose[6]*pose[6];if(Math.abs(norm-1)>.002)throw new IllegalArgumentException("Dropped quaternion");
  var basis=new Matrix4f(-1,0,0,0,0,0,-1,0,0,1,0,0,0,0,0,1);
  var rotation=new Matrix4f(basis).mul(new Matrix4f().rotation(new Quaternionf(pose[3],pose[4],pose[5],pose[6]))).mul(new Matrix4f(basis).transpose());
  var result=new Matrix4f().translation((float)(camera.x()-pose[0]),(float)(pose[2]-camera.height()),(float)(camera.y()-pose[1]))
   .mul(rotation).translate((float)(nativeOrigin.x()-camera.x()),(float)(camera.height()-nativeOrigin.height()),(float)(nativeOrigin.y()-camera.y()));
  if(!result.isFinite()||Math.abs(result.determinant()-1)>.002)throw new IllegalArgumentException("Dropped rigid transform");return result;
 }
 public static int applyRows(float[] rows,int[] flags,int[] palettes,int[] shadows,int first,int last,Matrix4f delta){
  if(rows==null||flags==null||palettes==null||shadows==null||first<0||last<first||last-first>16||last>rows.length/STRIDE||last>flags.length||last>palettes.length||last>shadows.length
    ||delta==null||!delta.isFinite()||Math.abs(delta.determinant()-1)>.002)throw new IllegalArgumentException("Dropped packet bounds");
  var indices=new ArrayList<Integer>();var updated=new ArrayList<float[]>();
  for(int i=first;i<last;i++){
   if(palettes[i]>=0||shadows[i]>=0)continue;
   var row=Arrays.copyOfRange(rows,i*STRIDE,i*STRIDE+STRIDE);var matrix=new Matrix4f().set(row,0);
   for(int f=24;f<28;f++)if(!Float.isFinite(row[f]))throw new IllegalArgumentException("Dropped bounds value");
   if(!matrix.isFinite()||row[27]<0)throw new IllegalArgumentException("Dropped packet pose");
   var moved=new Matrix4f(delta).mul(matrix);var centre=delta.transformPosition(new Vector3f(row[24],row[25],row[26]));
   if(!moved.isFinite()||!centre.isFinite())throw new IllegalArgumentException("Dropped packet overflow");
   moved.get(row,0);moved.get(row,36);row[24]=centre.x;row[25]=centre.y;row[26]=centre.z;
   indices.add(i);updated.add(row);
  }
  for(int i=0;i<indices.size();i++){int index=indices.get(i);System.arraycopy(updated.get(i),0,rows,index*STRIDE,STRIDE);flags[index]&=~8;}
  return indices.size();
 }
 public static int apply(Frame frame,int first,int last,ContactFrame.Point origin,float[] pose)throws ReflectiveOperationException{
  var fields=access;if(fields==null){fields=new Fields(field("values"),field("flags"),field("palette"),field("shadowPalette"));access=fields;}
  var rows=frame.scene.models;
  return applyRows((float[])fields.values.get(rows),(int[])fields.flags.get(rows),(int[])fields.palette.get(rows),(int[])fields.shadow.get(rows),first,last,
   delta(new ContactFrame.Point(frame.camX,frame.camY,frame.camZ*Math.sqrt(6)),origin,pose));
 }
 private static Field field(String name)throws ReflectiveOperationException{var f=ModelDraws.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private DroppedItemPose(){}
}
