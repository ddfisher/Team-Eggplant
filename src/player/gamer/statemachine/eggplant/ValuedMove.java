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

class CacheValue {
	public ValuedMove valuedMove;
	public int alpha;
	public int beta;
	
	public CacheValue(ValuedMove valuedMove, int alpha, int beta) {
		super();
		this.valuedMove = valuedMove;
		this.alpha = alpha;
		this.beta = beta;
	}
	
}