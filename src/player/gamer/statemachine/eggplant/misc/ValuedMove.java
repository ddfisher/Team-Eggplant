package player.gamer.statemachine.eggplant.misc;

import util.statemachine.Move;

public class ValuedMove{
  public int value;
  public Move move;
  public boolean terminal;
  public int depth;

  public ValuedMove(int value, Move move, int depth, boolean terminal){
    this.value = value;
    this.move = move;
    this.depth = depth;
    this.terminal = terminal;
  }
  
  public ValuedMove(int value, Move move, boolean terminal){
    this(value, move, -1, terminal); 
  }
  
  public ValuedMove(int value, Move move, int depth){
    this(value, move, depth, false); 
  }

  public ValuedMove(int value, Move move) {
    this(value, move, false);
  }

  @Override
  public String toString() {
    return this.move + " @ " + this.value + (this.terminal ? "T" : "") + ", d = " + this.depth;
  }
}