package player.gamer.statemachine.eggplant;

import util.statemachine.Move;

class ValuedMove{
	public int value;
	public Move move;
	public ValuedMove(int value, Move move){
		this.value=value;
		this.move = move;
	}
}