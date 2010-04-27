package player.gamer.statemachine.eggplant;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class FocusHeuristicEvaluator implements HeuristicEvaluator {
	
	private MobilityHeuristicEvaluator mob;
	
	public FocusHeuristicEvaluator(MobilityType type) {
		mob = new MobilityHeuristicEvaluator(type);
	}
	
	public FocusHeuristicEvaluator() {
		this(MobilityType.ONE_STEP);
	}
	
	public void updateAverage(int factor) {
		mob.updateAverage(factor);
	}
	
	public void setDepthLimit(int depthLimit) {
		mob.setDepthLimit(depthLimit);
	}

	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth) {
		return 100 - mob.eval(machine, state, role, alpha, beta, depth);
	}

}
