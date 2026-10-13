package pzvrphysicality;

import java.util.List;

/** Numeric-only copy of upstream rig contacts. Coordinates are world x/y and physical height. */
public record ContactFrame(long hostTime,long inputTime,List<Hand> hands) {
 public record Point(double x,double y,double height) {
  public Point {if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(height))throw new IllegalArgumentException("Nonfinite hand point");}
  public double distance(Point p){return Math.sqrt((x-p.x)*(x-p.x)+(y-p.y)*(y-p.y)+(height-p.height)*(height-p.height));}
 }
 public record Hand(int side,Point start,Point end,double radius,boolean grip,boolean trigger) {
  public Hand {if(side<0||side>1||start==null||end==null||!Double.isFinite(radius)||radius<=0||radius>.5||start.distance(end)>2)throw new IllegalArgumentException("Unsupported hand contact");}
  public Point centre(){return new Point((start.x+end.x)/2,(start.y+end.y)/2,(start.height+end.height)/2);}
 }
 public ContactFrame {hands=List.copyOf(hands);if(hands.size()>2||hands.stream().map(Hand::side).distinct().count()!=hands.size())throw new IllegalArgumentException("Duplicate/budgeted hands");}
 public boolean fresh(long now){return now>=hostTime&&now-hostTime<150_000_000L;}
 public Hand hand(int side){return hands.stream().filter(h->h.side==side).findFirst().orElse(null);}
}
