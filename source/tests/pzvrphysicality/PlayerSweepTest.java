package pzvrphysicality;
import java.util.*;
import viewpointvr.physics.CollisionShape;
public final class PlayerSweepTest {
 private static int checks;private static void check(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
 private static float[] pose(float x,float y,float z){return new float[]{x,y,z,0,0,0,1};}
 public static CollisionShape meshBox(float x,float y,float z,float cx,float cy,float cz){return new CollisionShape(new float[]{cx-x,cy-y,cz-z,cx+x,cy-y,cz-z,cx+x,cy+y,cz-z,cx-x,cy+y,cz-z,cx-x,cy-y,cz+z,cx+x,cy-y,cz+z,cx+x,cy+y,cz+z,cx-x,cy+y,cz+z},new int[]{0,1,2,0,2,3,4,6,5,4,7,6,0,4,5,0,5,1,1,5,6,1,6,2,2,6,7,2,7,3,3,7,4,3,4,0},new float[0],true);}
 private static PlayerSweep.Mesh table(){return PlayerSweep.read(meshBox(.5f,.5f,.04f,0,0,.8f),pose(0,0,0));}
 private static PlayerSweep.Result move(List<PlayerSweep.Mesh> geometry,double x,double y,double dx,double dy){return PlayerSweep.move(geometry,x,y,0,1.65,.18,dx,dy);}
 public static void main(String[] args){
  var table=table();var stop=move(List.of(table),-1,0,1.5,0);check(stop.x()<.33&&stop.x()>.30&&Math.abs(stop.y())<1e-8,"actual table top blocks horizontal avatar capsule before penetration");
  var slide=move(List.of(table),-1,0,1,.3);check(slide.x()<.33&&slide.y()>.29,"table contact preserves tangential motion");
  var corner=move(List.of(table),-1,-1,1,1);check(corner.x()<.39&&corner.y()<.39,"diagonal table corner stops at rounded body contact");
  var empty=move(List.of(),-1,0,1.5,0);check(empty.x()==1.5&&empty.y()==0,"missing geometry never adds an invisible full-tile barrier");
  var escaped=move(List.of(table),0,0,1,0);check(escaped.x()>.9,"initial overlap can escape instead of trapping an actor in stale geometry");
  var frame=List.of(PlayerSweep.read(meshBox(.06f,.5f,.85f,0,0,.85f),pose(0,-1,0)),PlayerSweep.read(meshBox(.06f,.5f,.85f,0,0,.85f),pose(0,1,0)));
  var through=move(frame,-1,0,1.5,0);check(through.x()==1.5,"open doorway aperture is not filled by outer bounds");
  var glass=new ArrayList<>(frame);glass.add(PlayerSweep.read(meshBox(.01f,.5f,.65f,0,0,.85f),pose(0,0,0)));check(move(glass,-1,0,1.5,0).x()<.82,"visible closed pane blocks avatar");
  check(move(frame,-1,0,1.5,0).x()==1.5,"open or removed pane retires only its actual triangles");
  var floor=PlayerSweep.read(meshBox(2,2,.02f,0,0,-.02f),pose(0,0,0));check(move(List.of(floor),0,0,1,0).x()==1,"native floor surface is not a horizontal wall");
  var roof=PlayerSweep.read(meshBox(2,2,.02f,0,0,2.2f),pose(0,0,0));check(move(List.of(roof),0,0,1,0).x()==1,"roof above head leaves native walking free");
  var shortBody=PlayerSweep.move(List.of(table),-1,0,0,.55,.18,1.5,0);check(shortBody.x()==1.5,"actual crouched body can move under a table with sufficient clearance");
  var moved=PlayerSweep.read(meshBox(.5f,.5f,.04f,0,0,.8f),pose(4,0,0));check(move(List.of(moved),-1,0,1.5,0).x()==1.5,"translated actual geometry has no stale contact at its old mount");
  var remote=PlayerSweep.read(meshBox(.5f,.5f,.04f,0,0,.8f),pose(12000.5f,9000.5f,0));check(move(List.of(remote),11999.5,9000.5,1.5,0).x()<.33,"large map coordinates retain body contact clearance");
  var wall=PlayerSweep.read(new CollisionShape(new float[]{0,-2,0,0,2,0,0,2,2,0,-2,2},new int[]{0,1,2,0,2,3},new float[0],true),pose(0,0,0));check(move(List.of(wall),-1,0,1.5,0).x()<.82,"zero-thickness rendered wall triangles work without fabricated thickness");
  check(move(List.of(wall),-.18,0,-.3,0).x()==-.3,"near-contact motion away from a wall is retained");
  check(move(List.of(wall),0,0,-.3,0).x()==-.3,"initially coincident thin wall cannot trap native actor escape");
  check(wall.planeDepth()==0&&table.planeDepth()>.05,"reported actual mesh depth distinguishes a plane from an extruded table");
  boolean proxy=false;try{PlayerSweep.read(CollisionShape.box(.5f,.5f,.5f,0,0,.5f),pose(0,0,0));}catch(IllegalArgumentException expected){proxy=true;}check(proxy,"generic full-tile box proxy is not accepted as rendered triangle evidence");
  var dense=new ArrayList<PlayerSweep.Triangle>();for(int i=0;i<PlayerSweep.MAX_TRIANGLES+1;i++)dense.add(new PlayerSweep.Triangle(0,-2,0,0,2,0,0,2,2));var result=move(List.of(new PlayerSweep.Mesh(dense,0,0,-2,2,0,2)),-1,0,1.5,0);check(result.budget()&&result.x()==1.5,"budget exhaustion omits collision instead of inventing a stop");
  boolean invalid=false;try{PlayerSweep.move(List.of(),0,0,0,1,.18,Double.NaN,0);}catch(IllegalArgumentException expected){invalid=true;}check(invalid,"invalid movement is rejected by pure query");
  System.out.println("Player triangle sweep: "+checks+" table, sliding, openings, actual geometry and omission checks passed");
 }
}
