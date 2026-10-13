package pzvrphysicality;

/** A released short trigger pages; a new long trigger followed by grip release suspends the held bag. */
public final class BackpackGesture {
 public enum Action {NONE,PAGE,SUSPEND}
 private boolean neutral,down,hadGrip,used;private long began;
 public Action sample(boolean trigger,boolean grip,boolean eligible,long now){if(!eligible){reset();return Action.NONE;}if(down&&now<began){reset();return Action.NONE;}if(!trigger){var action=down&&!used&&now-began<700_000_000L&&grip?Action.PAGE:Action.NONE;neutral=true;down=hadGrip=used=false;return action;}if(!down){if(!neutral)return Action.NONE;down=true;began=now;hadGrip=grip;used=false;}hadGrip|=grip;if(!used&&hadGrip&&!grip&&now-began>=700_000_000L){used=true;return Action.SUSPEND;}return Action.NONE;}
 public boolean preparing(long now){return down&&!used&&now>=began&&now-began>=300_000_000L;}
 public boolean candidate(boolean trigger,boolean grip,long now){return trigger&&!grip&&down&&!used&&hadGrip&&now>=began&&now-began>=700_000_000L;}
 public void reset(){neutral=down=hadGrip=used=false;began=0;}
}
