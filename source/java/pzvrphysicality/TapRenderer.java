package pzvrphysicality;

import java.util.*;import org.joml.Matrix4f;import static org.lwjgl.opengl.GL33.*;

/** Owned, depth-tested tap markers and water geometry. Borrows depth through a private sampler only. */
public final class TapRenderer implements AutoCloseable {
 private int program,vao,vbo,sampler;
 public static MouthMath.Vec renderPoint(ContactFrame.Point world,ContactFrame.Point camera){return new MouthMath.Vec(camera.x()-world.x(),world.height()-camera.height(),camera.y()-world.y());}
 public void draw(TapVisuals visuals,ContactFrame.Point camera,Matrix4f projection,int depth,int width,int height){
  if(visuals==null||visuals.dots().isEmpty()&&visuals.streams().isEmpty())return;
  if(camera==null||projection==null||!projection.isFinite()||width<1||height<1||!glIsTexture(depth))throw new IllegalArgumentException("Tap output/depth scope");
  int draw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING),read=glGetInteger(GL_READ_FRAMEBUFFER_BINDING),oldProgram=glGetInteger(GL_CURRENT_PROGRAM),oldVao=glGetInteger(GL_VERTEX_ARRAY_BINDING),oldBuffer=glGetInteger(GL_ARRAY_BUFFER_BINDING),active=glGetInteger(GL_ACTIVE_TEXTURE);
  int[] viewport=new int[4];glGetIntegerv(GL_VIEWPORT,viewport);glActiveTexture(GL_TEXTURE0);int texture=glGetInteger(GL_TEXTURE_BINDING_2D),oldSampler=glGetIntegeri(GL_SAMPLER_BINDING,0);boolean points=glIsEnabled(GL_PROGRAM_POINT_SIZE),srgb=glIsEnabled(GL_FRAMEBUFFER_SRGB),discard=glIsEnabled(GL_RASTERIZER_DISCARD);
  if(viewport[2]!=width||viewport[3]!=height){glActiveTexture(active);throw new IllegalArgumentException("Tap viewport extent differs from native output");}
  glPushAttrib(GL_ALL_ATTRIB_BITS);
  try{
   if(program==0)init();glUseProgram(program);glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glEnable(GL_PROGRAM_POINT_SIZE);glEnable(GL_POINT_SPRITE);
   glDisable(GL_DEPTH_TEST);glDepthMask(false);glDisable(GL_STENCIL_TEST);glDisable(GL_SCISSOR_TEST);glDisable(GL_CULL_FACE);glDisable(GL_ALPHA_TEST);glDisable(GL_FRAMEBUFFER_SRGB);glDisable(GL_COLOR_LOGIC_OP);glDisable(GL_RASTERIZER_DISCARD);glColorMask(true,true,true,true);
   glEnable(GL_BLEND);glBlendEquationSeparate(GL_FUNC_ADD,GL_FUNC_ADD);glBlendFuncSeparate(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
   glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,depth);glBindSampler(0,sampler);
   glUniform1i(glGetUniformLocation(program,"sceneDepth"),0);glUniform2f(glGetUniformLocation(program,"viewportOrigin"),viewport[0],viewport[1]);glUniform2f(glGetUniformLocation(program,"outputSize"),width,height);
   glUniformMatrix4fv(glGetUniformLocation(program,"viewProjection"),false,projection.get(new float[16]));glUniform1f(glGetUniformLocation(program,"pointScale"),Math.min(width,height)*.012f);
   var dots=new float[visuals.dots().size()*7];int index=0;for(var dot:visuals.dots()){var point=renderPoint(dot.point(),camera);index=vertex(dots,index,point.x(),point.y(),point.z(),1,1,1,dot.hovered()?1:.75f);}
   glUniform1i(glGetUniformLocation(program,"pointMode"),1);upload(dots);glDrawArrays(GL_POINTS,0,dots.length/7);
   var streams=new float[visuals.streams().size()*12*6*7];index=0;
   for(var stream:visuals.streams()){
    if(!Double.isFinite(stream.units())||stream.units()<.25||stream.units()>4)throw new IllegalArgumentException("Tap stream scale");
    double radius=.003*stream.units();for(int segment=0;segment<12;segment++){
     double a=segment*Math.PI/6,b=(segment+1)*Math.PI/6;
     var topA=renderPoint(new ContactFrame.Point(stream.start().x()+Math.cos(a)*radius,stream.start().y()+Math.sin(a)*radius,stream.start().height()),camera);
     var topB=renderPoint(new ContactFrame.Point(stream.start().x()+Math.cos(b)*radius,stream.start().y()+Math.sin(b)*radius,stream.start().height()),camera);
     var bottomA=renderPoint(new ContactFrame.Point(stream.end().x()+Math.cos(a)*radius,stream.end().y()+Math.sin(a)*radius,stream.end().height()),camera);
     var bottomB=renderPoint(new ContactFrame.Point(stream.end().x()+Math.cos(b)*radius,stream.end().y()+Math.sin(b)*radius,stream.end().height()),camera);
     for(var point:List.of(topA,bottomA,topB,topB,bottomA,bottomB))index=vertex(streams,index,point.x(),point.y(),point.z(),.38f,.73f,1,.7f);
    }
   }
   glUniform1i(glGetUniformLocation(program,"pointMode"),0);upload(streams);glDrawArrays(GL_TRIANGLES,0,streams.length/7);
  }finally{
   glBindFramebuffer(GL_DRAW_FRAMEBUFFER,draw);glBindFramebuffer(GL_READ_FRAMEBUFFER,read);glPopAttrib();
   glUseProgram(oldProgram);glBindVertexArray(oldVao);glBindBuffer(GL_ARRAY_BUFFER,oldBuffer);glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,texture);glBindSampler(0,oldSampler);glActiveTexture(active);
   if(points)glEnable(GL_PROGRAM_POINT_SIZE);else glDisable(GL_PROGRAM_POINT_SIZE);
   if(srgb)glEnable(GL_FRAMEBUFFER_SRGB);else glDisable(GL_FRAMEBUFFER_SRGB);
   if(discard)glEnable(GL_RASTERIZER_DISCARD);else glDisable(GL_RASTERIZER_DISCARD);
  }
 }
 private static int vertex(float[] data,int index,double x,double y,double z,float r,float g,float b,float a){data[index++]=(float)x;data[index++]=(float)y;data[index++]=(float)z;data[index++]=r;data[index++]=g;data[index++]=b;data[index++]=a;return index;}
 private void upload(float[] data){glBufferData(GL_ARRAY_BUFFER,data,GL_STREAM_DRAW);}
 private void init(){int vertex=0,fragment=0,p=0;try{
  vertex=shader(GL_VERTEX_SHADER,"#version 330\nlayout(location=0)in vec3 position;layout(location=1)in vec4 colour;uniform mat4 viewProjection;uniform float pointScale;out vec4 tint;void main(){gl_Position=viewProjection*vec4(position,1);gl_PointSize=clamp(pointScale/max(.05,gl_Position.w),3,18);tint=colour;}");
  fragment=shader(GL_FRAGMENT_SHADER,"#version 330\nin vec4 tint;uniform sampler2D sceneDepth;uniform vec2 viewportOrigin,outputSize;uniform int pointMode;out vec4 color;void main(){if(pointMode==1&&length(gl_PointCoord-vec2(.5))>.5)discard;float depth=texture(sceneDepth,(gl_FragCoord.xy-viewportOrigin)/outputSize).r;if(gl_FragCoord.z>depth+.00002)discard;color=tint;}");
  p=glCreateProgram();glAttachShader(p,vertex);glAttachShader(p,fragment);glLinkProgram(p);if(glGetProgrami(p,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(p));
  vao=glGenVertexArrays();vbo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glEnableVertexAttribArray(0);glVertexAttribPointer(0,3,GL_FLOAT,false,28,0L);glEnableVertexAttribArray(1);glVertexAttribPointer(1,4,GL_FLOAT,false,28,12L);
  sampler=glGenSamplers();glSamplerParameteri(sampler,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glSamplerParameteri(sampler,GL_TEXTURE_MAG_FILTER,GL_NEAREST);glSamplerParameteri(sampler,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glSamplerParameteri(sampler,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);glSamplerParameteri(sampler,GL_TEXTURE_COMPARE_MODE,GL_NONE);program=p;p=0;
  }catch(RuntimeException|LinkageError error){close();throw error;}
  finally{if(vertex!=0)glDeleteShader(vertex);if(fragment!=0)glDeleteShader(fragment);if(p!=0)glDeleteProgram(p);}}
 private static int shader(int type,String text){int s=glCreateShader(type);glShaderSource(s,text);glCompileShader(s);if(glGetShaderi(s,GL_COMPILE_STATUS)==0){String error=glGetShaderInfoLog(s);glDeleteShader(s);throw new IllegalStateException(error);}return s;}
 public void close(){if(program!=0)glDeleteProgram(program);if(vao!=0)glDeleteVertexArrays(vao);if(vbo!=0)glDeleteBuffers(vbo);if(sampler!=0)glDeleteSamplers(sampler);program=vao=vbo=sampler=0;}
}
