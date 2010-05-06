package player.gamer.statemachine.eggplant.heuristic;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class OpponentFocusHeuristic extends OpponentMobilityHeuristic {

	public OpponentFocusHeuristic(MobilityType type, int numPlayers) {
		super(type, numPlayers);
	}
	
	public OpponentFocusHeuristic(MobilityType type, int numPlayers, int depthLimit) {
		super(type, numPlayers, depthLimit);
	}
	
	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth, long endTime) {
		return 100 - super.eval(machine, state, role, alpha, beta, depth, absDepth, endTime);
	}

}
