package player.gamer.statemachine.eggplant.misc;

import util.statemachine.Move;

public class ValuedMove{
	public int value;
	public Move move;
	public boolean terminal;
	public ValuedMove(int value, Move move, boolean terminal){
		this.value=value;
		this.move = move;
		this.terminal = terminal;
	}
	public ValuedMove(int value, Move move) {
		this(value, move, true);
	}
	
	@Override
	public String toString() {
	  return this.move + " @ " + this.value;
	}
}