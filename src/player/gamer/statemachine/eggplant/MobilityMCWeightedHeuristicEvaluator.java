package player.gamer.statemachine.eggplant;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class MobilityMCWeightedHeuristicEvaluator implements HeuristicEvaluator, MobilityTracker{
	
	MobilityHeuristicEvaluator mob;
	MonteCarloHeuristicEvaluator mc;
	private static final int DEFAULT_TRIALS = 4;
	
	public MobilityMCWeightedHeuristicEvaluator(MobilityType type, int trials) {
		mob = new MobilityHeuristicEvaluator(type);
		mc = new MonteCarloHeuristicEvaluator(trials);
	}
	
	public MobilityMCWeightedHeuristicEvaluator() { this(MobilityType.ONE_STEP, DEFAULT_TRIALS); }

	@Override
	public int eval(StateMachine machine, MachineState state, Role role,
			int alpha, int beta, int depth) {
		return (int)Math.round((mob.eval(machine, state, role, alpha, beta, depth) + 
				mc.eval(machine, state, role, alpha, beta, depth)) / 2.0);
	}

	@Override
	public int depthLimit() {
		return mob.depthLimit();
	}

	@Override
	public void setDepthLimit(int depthLimit) {
		mob.setDepthLimit(depthLimit);
	}

	@Override
	public void updateAverage(int factor) {
		mob.updateAverage(factor);
	}

}
