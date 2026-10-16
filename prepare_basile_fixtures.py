"""Private numeric test inputs from separately installed original provider assets. No asset export."""
from pathlib import Path
import argparse,hashlib,json,struct,math
OWNER='f7334ddeb1ff4a8b9963f24215c1d61080becea59c7779a54df2a9c22254d417'
ASSETS={'garage-closed': ('garage_white_garage_w_0_closed', '9ca947ace194b3d4ce37702b8b3ebcfb2ec41ddb40b0351f23aa194e4f69c3a3'), 'garage-open': ('garage_white_garage_w_0_open', 'b4b49926f31deb89dfcacc26aac3ef19f3278d783d93caa863f42b7ab524f60a'), 'native-leaf': ('carpentry_carpentry_01_48', '1b1b0b1a23d271b9b0b0a29580047eca0397eb23edd46695f24ba2ff1b6f76ff')}
def plain(path):
 path=Path(path).absolute()
 for p in [path,*path.parents]:
  if p.exists() and p.stat(follow_symlinks=False).st_file_attributes&0x400:raise ValueError('Reparse source/output rejected')
 if path.exists() and path.resolve()!=path:raise ValueError('Source/output alias rejected')
 return path

def bounded(path,maximum,expected):
 path=plain(path)
 with path.open('rb')as f:data=f.read(maximum+1)
 if len(data)>maximum or hashlib.sha256(data).hexdigest()!=expected:raise ValueError('Unverified provider source bytes')
 return data

def decode(data):
 if len(data)<28:raise ValueError('GLB extent')
 magic,version,total,n,kind=struct.unpack_from('<IIIII',data)
 if magic!=0x46546c67 or version!=2 or total!=len(data) or kind!=0x4e4f534a or n>512*1024 or n+28>len(data):raise ValueError('GLB header')
 j=json.loads(data[20:20+n]);size,tag=struct.unpack_from('<II',data,20+n)
 if tag!=0x004e4942 or 28+n+size!=len(data) or len(j['meshes'])!=1 or len(j['meshes'][0]['primitives'])!=1:raise ValueError('GLB body')
 blob=data[28+n:];q=j['meshes'][0]['primitives'][0]
 if q.get('mode',4)!=4 or len(j['skins'])!=1 or len(j['skins'][0]['joints'])!=1:raise ValueError('Unsupported source layout')
 def values(index):
  a=j['accessors'][index];v=j['bufferViews'][a['bufferView']]
  if a.get('sparse')or v.get('buffer',0)!=0:raise ValueError('Sparse/external source')
  fmt={5126:'f',5125:'I',5123:'H',5121:'B'}[a['componentType']];arity={'SCALAR':1,'VEC3':3,'VEC4':4}[a['type']];step=struct.calcsize(fmt)*arity;stride=v.get('byteStride',step);start=v.get('byteOffset',0)+a.get('byteOffset',0);count=a['count']
  if count<1 or count>48000 or stride<step or start<0 or start+(count-1)*stride+step>len(blob):raise ValueError('Accessor budget')
  return [z for i in range(count)for z in struct.unpack_from('<'+fmt*arity,blob,start+i*stride)]
 pos=values(q['attributes']['POSITION']);faces=values(q['indices']);weights=values(q['attributes']['WEIGHTS_0']);joints=values(q['attributes']['JOINTS_0']);v=len(pos)//3
 if len(pos)%3 or v<4 or v>8000 or len(faces)%3 or len(faces)>48000 or len(weights)!=v*4 or len(joints)!=v*4 or any(not math.isfinite(x)or abs(x)>16 for x in pos)or any(i<0 or i>=v for i in faces):raise ValueError('Numeric budget')
 for i in range(v):
  w=weights[i*4:i*4+4]
  if any(not math.isfinite(x)or x<0 or x>1.002 for x in w)or abs(sum(w)-1)>.002 or any(b<0 or b>=64 for b in joints[i*4:i*4+4]):raise ValueError('Skin data')
 return pos,faces,weights,joints

def main():
 a=argparse.ArgumentParser();a.add_argument('--mod-directory',required=True);a.add_argument('--out',required=True);args=a.parse_args();mod=plain(Path(args.mod_directory));out=plain(Path(args.out))
 if out.exists():raise ValueError('Fresh private numeric output required')
 jar=mod/'42/media/java/client/BasileDoors3D.jar';bounded(jar,65536,OWNER)
 result={'ProviderJarSHA256':OWNER,'OriginalMethodsExecuted':False,'PrivateNumericOnly':True,'Assets':{}};out.mkdir()
 for label,(key,want)in ASSETS.items():
  original=mod/'42/media/models_X/BasileDoors3D'/(key+'.glb');data=bounded(original,1024*1024,want);pos,faces,weights,joints=decode(data);target=out/(label+'.bin')
  with target.open('wb')as f:
   f.write(struct.pack('>ii',len(pos)//3,len(faces)))
   for fmt,values in [('f',pos),('i',faces),('f',weights),('i',joints)]:f.write(struct.pack('>'+fmt*len(values),*values))
  result['Assets'][label]={'SourceSHA256':want,'NumericSHA256':hashlib.sha256(target.read_bytes()).hexdigest(),'Vertices':len(pos)//3,'Indices':len(faces),'OriginalFile':str(original),'NumericFile':str(target)}
 (out/'readback.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
if __name__=='__main__':main()
