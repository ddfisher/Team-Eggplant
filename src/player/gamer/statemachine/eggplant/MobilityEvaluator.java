package player.gamer.statemachine.eggplant;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class MobilityEvaluator implements HeuristicEvaluator {
	
	private double averageBranching;
	private int samples = 0;

	@Override
	public int eval(StateMachine machine, MachineState state, Role role) {
		double avgLocalBranching = 5;
		return 
		// TODO Auto-generated method stub
		return 100;
	}
	
	public void updateAverage(int factor) {
		averageBranching = samples / (double)(samples + 1) * factor / (double)(samples + 1);
		samples++;
	}

}
