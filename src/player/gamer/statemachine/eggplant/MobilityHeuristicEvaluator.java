package player.gamer.statemachine.eggplant;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class MobilityHeuristicEvaluator implements HeuristicEvaluator {

	private double averageBranching;
	private int samples = 0;
	
	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth) {
		// TODO Auto-generated method stub
		return (alpha + beta) / 2;
	}
	
	public void updateAverage(int factor) {
		averageBranching = samples / (double)(samples + 1) * factor / (double)(samples + 1);
		samples++;
	}

}
