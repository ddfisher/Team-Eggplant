package player.gamer.statemachine.eggplant;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class MobilityHeuristicEvaluator implements HeuristicEvaluator {

	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth) {
		// TODO Auto-generated method stub
		return (alpha + beta) / 2;
	}

}
