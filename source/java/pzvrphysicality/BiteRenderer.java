package pzvrphysicality;

import org.joml.Matrix4f;import static org.lwjgl.opengl.GL33.*;

/** Private textured portion draw in the original perspective/depth scope. */
public final class BiteRenderer implements AutoCloseable {
 private int program,vao,vbo,depthSampler,foodSampler;
 public void draw(BiteGeometry.Portion portion,Matrix4f mount,Matrix4f projection,int texture,float[] tint,float[] uvBounds,int depth,int width,int height){
  if(portion==null||mount==null||projection==null||!mount.isFinite()||!projection.isFinite()||Math.abs(mount.determinant())<1e-10f||!glIsTexture(texture)||!glIsTexture(depth)||texture==depth||width<1||height<1||tint==null||tint.length!=3||uvBounds==null||uvBounds.length!=4)throw new IllegalArgumentException("Bite render scope");
  for(float value:tint)if(!Float.isFinite(value)||value<0||value>1)throw new IllegalArgumentException("Bite tint");
  for(float value:uvBounds)if(!Float.isFinite(value)||value<0||value>1)throw new IllegalArgumentException("Bite texture bounds");if(uvBounds[0]>=uvBounds[2]||uvBounds[1]>=uvBounds[3])throw new IllegalArgumentException("Collapsed bite texture bounds");
  int draw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING),read=glGetInteger(GL_READ_FRAMEBUFFER_BINDING),oldProgram=glGetInteger(GL_CURRENT_PROGRAM),oldVao=glGetInteger(GL_VERTEX_ARRAY_BINDING),oldBuffer=glGetInteger(GL_ARRAY_BUFFER_BINDING),active=glGetInteger(GL_ACTIVE_TEXTURE);
  int[] viewport=new int[4];glGetIntegerv(GL_VIEWPORT,viewport);if(viewport[2]!=width||viewport[3]!=height)throw new IllegalArgumentException("Bite viewport extent");
  glActiveTexture(GL_TEXTURE0);int oldDepth=glGetInteger(GL_TEXTURE_BINDING_2D),oldDepthSampler=glGetIntegeri(GL_SAMPLER_BINDING,0);glActiveTexture(GL_TEXTURE1);int oldFood=glGetInteger(GL_TEXTURE_BINDING_2D),oldFoodSampler=glGetIntegeri(GL_SAMPLER_BINDING,1);glActiveTexture(active);
  boolean srgb=glIsEnabled(GL_FRAMEBUFFER_SRGB),discard=glIsEnabled(GL_RASTERIZER_DISCARD);glPushAttrib(GL_ALL_ATTRIB_BITS);
  try{
   if(program==0)init();glUseProgram(program);glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);float[] data=portion.triangles();glBufferData(GL_ARRAY_BUFFER,data,GL_STREAM_DRAW);
   glDisable(GL_DEPTH_TEST);glDepthMask(false);glDisable(GL_STENCIL_TEST);glDisable(GL_SCISSOR_TEST);glDisable(GL_CULL_FACE);glDisable(GL_ALPHA_TEST);glDisable(GL_FRAMEBUFFER_SRGB);glDisable(GL_COLOR_LOGIC_OP);glDisable(GL_RASTERIZER_DISCARD);glColorMask(true,true,true,true);
   glEnable(GL_BLEND);glBlendEquationSeparate(GL_FUNC_ADD,GL_FUNC_ADD);glBlendFuncSeparate(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
   glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,depth);glBindSampler(0,depthSampler);glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,texture);glBindSampler(1,foodSampler);
   glUniform1i(glGetUniformLocation(program,"sceneDepth"),0);glUniform1i(glGetUniformLocation(program,"foodTexture"),1);glUniform2f(glGetUniformLocation(program,"viewportOrigin"),viewport[0],viewport[1]);glUniform2f(glGetUniformLocation(program,"outputSize"),width,height);
   glUniform3f(glGetUniformLocation(program,"itemTint"),tint[0],tint[1],tint[2]);glUniform4f(glGetUniformLocation(program,"uvBounds"),uvBounds[0],uvBounds[1],uvBounds[2],uvBounds[3]);glUniformMatrix4fv(glGetUniformLocation(program,"mount"),false,mount.get(new float[16]));glUniformMatrix4fv(glGetUniformLocation(program,"viewProjection"),false,projection.get(new float[16]));glDrawArrays(GL_TRIANGLES,0,data.length/9);
  }finally{
   glBindFramebuffer(GL_DRAW_FRAMEBUFFER,draw);glBindFramebuffer(GL_READ_FRAMEBUFFER,read);glPopAttrib();glUseProgram(oldProgram);glBindVertexArray(oldVao);glBindBuffer(GL_ARRAY_BUFFER,oldBuffer);
   glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,oldDepth);glBindSampler(0,oldDepthSampler);glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,oldFood);glBindSampler(1,oldFoodSampler);glActiveTexture(active);
   if(srgb)glEnable(GL_FRAMEBUFFER_SRGB);else glDisable(GL_FRAMEBUFFER_SRGB);if(discard)glEnable(GL_RASTERIZER_DISCARD);else glDisable(GL_RASTERIZER_DISCARD);
  }
 }
 private void init(){int vs=0,fs=0,p=0;try{
  vs=shader(GL_VERTEX_SHADER,"#version 330\nlayout(location=0)in vec3 position;layout(location=1)in vec3 normal;layout(location=2)in vec2 uv;layout(location=3)in float cap;uniform mat4 mount,viewProjection;out vec2 tex;out vec3 n;out float cut;void main(){gl_Position=viewProjection*mount*vec4(position,1);n=mat3(transpose(inverse(mount)))*normal;tex=uv;cut=cap;}");
  fs=shader(GL_FRAGMENT_SHADER,"#version 330\nin vec2 tex;in vec3 n;in float cut;uniform sampler2D sceneDepth,foodTexture;uniform vec2 viewportOrigin,outputSize;uniform vec4 uvBounds;uniform vec3 itemTint;out vec4 colour;void main(){float d=texture(sceneDepth,(gl_FragCoord.xy-viewportOrigin)/outputSize).r;if(gl_FragCoord.z>d+.00002)discard;vec4 t=texture(foodTexture,mix(uvBounds.xy,uvBounds.zw,tex));if(t.a<.1)discard;float light=.72+.28*abs(dot(normalize(n),normalize(vec3(.3,.8,.4))));vec3 surface=mix(t.rgb,mix(t.rgb,vec3(.88,.78,.57),.3),clamp(cut,0,1));colour=vec4(surface*itemTint*light,t.a);}");
  p=glCreateProgram();glAttachShader(p,vs);glAttachShader(p,fs);glLinkProgram(p);if(glGetProgrami(p,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(p));
  vao=glGenVertexArrays();vbo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);for(int attribute=0;attribute<4;attribute++)glEnableVertexAttribArray(attribute);
  glVertexAttribPointer(0,3,GL_FLOAT,false,36,0L);glVertexAttribPointer(1,3,GL_FLOAT,false,36,12L);glVertexAttribPointer(2,2,GL_FLOAT,false,36,24L);glVertexAttribPointer(3,1,GL_FLOAT,false,36,32L);
  depthSampler=glGenSamplers();foodSampler=glGenSamplers();for(int s:new int[]{depthSampler,foodSampler}){glSamplerParameteri(s,GL_TEXTURE_MIN_FILTER,s==depthSampler?GL_NEAREST:GL_LINEAR);glSamplerParameteri(s,GL_TEXTURE_MAG_FILTER,s==depthSampler?GL_NEAREST:GL_LINEAR);glSamplerParameteri(s,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glSamplerParameteri(s,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);glSamplerParameteri(s,GL_TEXTURE_COMPARE_MODE,GL_NONE);}program=p;p=0;
 }catch(RuntimeException|LinkageError failure){close();throw failure;}finally{if(vs!=0)glDeleteShader(vs);if(fs!=0)glDeleteShader(fs);if(p!=0)glDeleteProgram(p);}}
 private static int shader(int type,String source){int id=glCreateShader(type);glShaderSource(id,source);glCompileShader(id);if(glGetShaderi(id,GL_COMPILE_STATUS)==0){String error=glGetShaderInfoLog(id);glDeleteShader(id);throw new IllegalStateException(error);}return id;}
 public void close(){if(program!=0)glDeleteProgram(program);if(vao!=0)glDeleteVertexArrays(vao);if(vbo!=0)glDeleteBuffers(vbo);if(depthSampler!=0)glDeleteSamplers(depthSampler);if(foodSampler!=0)glDeleteSamplers(foodSampler);program=vao=vbo=depthSampler=foodSampler=0;}
}
