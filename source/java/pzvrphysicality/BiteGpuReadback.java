package pzvrphysicality;

import java.nio.*;import static org.lwjgl.opengl.GL33.*;

/** Small read-only recovery for already-uploaded rigid meshes. No game or XR implementation is needed. */
public final class BiteGpuReadback {
 public record Recovery(BiteGeometry.Mesh mesh,float[] positions,int[] faces){public Recovery{positions=positions.clone();faces=faces.clone();}public float[] positions(){return positions.clone();}public int[] faces(){return faces.clone();}}
 public static Recovery read(int vertices,int elements,int stride,int position,int normal,int uv,int count){
  if(vertices<=0||elements<=0||vertices==elements||!glIsBuffer(vertices)||!glIsBuffer(elements)||stride<32||stride>128||stride%4!=0||position<0||normal<0||uv<0||position+12>stride||normal+12>stride||uv+8>stride||position%4!=0||normal%4!=0||uv%4!=0||position==normal||position==uv||normal==uv||count<3||count>12288||count%3!=0)throw new IllegalArgumentException("Rigid bite buffer layout");
  if(position<normal+12&&normal<position+12||position<uv+8&&uv<position+12||normal<uv+8&&uv<normal+12)throw new IllegalArgumentException("Overlapping bite vertex attributes");
  int previous=glGetInteger(GL_COPY_READ_BUFFER);
  try{
   glBindBuffer(GL_COPY_READ_BUFFER,vertices);int bytes=glGetBufferParameteri(GL_COPY_READ_BUFFER,GL_BUFFER_SIZE);
   if(bytes<stride*3||bytes>stride*4096||bytes%stride!=0)throw new IllegalArgumentException("Rigid bite vertex budget");
   int vertexCount=bytes/stride;var source=ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());glGetBufferSubData(GL_COPY_READ_BUFFER,0,source);
   glBindBuffer(GL_COPY_READ_BUFFER,elements);int indexBytes=glGetBufferParameteri(GL_COPY_READ_BUFFER,GL_BUFFER_SIZE);if(indexBytes!=count*4)throw new IllegalArgumentException("Rigid bite index extent");
   var indices=ByteBuffer.allocateDirect(indexBytes).order(ByteOrder.nativeOrder());glGetBufferSubData(GL_COPY_READ_BUFFER,0,indices);
   var data=new float[vertexCount*8];var positions=new float[vertexCount*3];var faces=new int[count];
   for(int v=0;v<vertexCount;v++){
    for(int axis=0;axis<3;axis++){positions[v*3+axis]=data[v*8+axis]=source.getFloat(v*stride+position+axis*4);data[v*8+3+axis]=source.getFloat(v*stride+normal+axis*4);}
    data[v*8+6]=source.getFloat(v*stride+uv);data[v*8+7]=source.getFloat(v*stride+uv+4);
   }
   for(int n=0;n<count;n++)faces[n]=indices.getInt(n*4);return new Recovery(new BiteGeometry.Mesh(data,faces),positions,faces);
  }finally{glBindBuffer(GL_COPY_READ_BUFFER,previous);}
 }
 private BiteGpuReadback(){}
}
