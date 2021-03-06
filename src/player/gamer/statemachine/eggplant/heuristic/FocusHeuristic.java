package player.gamer.statemachine.eggplant.heuristic;

import player.gamer.statemachine.eggplant.misc.TimeUpException;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class FocusHeuristic extends MobilityHeuristic implements Heuristic {

	public FocusHeuristic(MobilityType type, int numPlayers) {
		super(type, numPlayers);
	}

	@Override
	public int eval(StateMachine machine, MachineState state, Role role,
			int alpha, int beta, int depth, int absDepth, long endTime) throws TimeUpException {
		return 100 - super.eval(machine, state, role, alpha, beta, depth, absDepth, endTime);
	}
	
}
