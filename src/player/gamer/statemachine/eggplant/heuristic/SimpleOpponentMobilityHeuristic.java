package player.gamer.statemachine.eggplant.heuristic;

import java.util.List;

import player.gamer.statemachine.eggplant.misc.TimeUpException;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;

public class SimpleOpponentMobilityHeuristic implements Heuristic {
	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth, long timeout)
			throws MoveDefinitionException, TimeUpException {
		List<Role> roles = machine.getRoles();
		int product = 1;
		for (Role r : roles) {
			if (!r.equals(role))
				product *= machine.getLegalMoves(state, r).size();
		}
		return product;
	}

	@Override
	public void update(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth)
			throws MoveDefinitionException {
		// noop
	}
}