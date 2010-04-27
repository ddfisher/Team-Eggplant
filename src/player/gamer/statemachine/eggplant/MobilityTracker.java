package player.gamer.statemachine.eggplant;

public interface MobilityTracker {

	/* Updates the branching factor average with the given sample.
	 * Should be avoided on noop-only moves to bring average for legitimate move choices closer to 50.
	 */
	public void updateAverage(int factor);
	
	/* Setter and getter for depthLimit, which controls how far down the evaluator goes in
	 * n-step mobility evaluation.
	 */
	public int depthLimit();
	public void setDepthLimit(int depthLimit);
}
