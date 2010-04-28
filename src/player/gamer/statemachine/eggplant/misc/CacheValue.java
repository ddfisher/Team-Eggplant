package player.gamer.statemachine.eggplant.misc;

public class CacheValue {
	public ValuedMove valuedMove;
	public int alpha;
	public int beta;
	
	public CacheValue(ValuedMove valuedMove, int alpha, int beta) {
		super();
		this.valuedMove = valuedMove;
		this.alpha = alpha;
		this.beta = beta;
	}
	
	@Override
    public String toString() {
      return this.valuedMove + " : (" + this.alpha + ", " + this.beta + ")";
    }
}