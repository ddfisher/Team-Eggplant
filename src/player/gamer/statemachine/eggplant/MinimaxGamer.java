package player.gamer.statemachine.eggplant;

import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.cache.CachedProverStateMachine;
import apps.player.detail.DetailPanel;

public class MinimaxGamer extends StateMachineGamer {
	private int statesSearched;
	private int leafNodesSearched;

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// do nothing
	}

	/* Implements Minimax search, currently ignores clock */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		leafNodesSearched = statesSearched = 0;

		ValuedMove result = minimax(getStateMachine(), getCurrentState(), getRole());

		long stop = System.currentTimeMillis();
		notifyObservers(new EggplantMoveSelectionEvent(statesSearched, leafNodesSearched, stop - start, result.value, result.move));
		return result.move;
	}

	private ValuedMove minimax(StateMachine machine, MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException,
			GoalDefinitionException {
		statesSearched++;
		if (machine.isTerminal(state)) {
			leafNodesSearched++;
			return new ValuedMove(machine.getGoal(state, role), null);
		}

		ValuedMove maxMove = new ValuedMove(-1, null);
		List<Move> possibleMoves = machine.getLegalMoves(state, role);
		for (Move move : possibleMoves) {
			List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
			int min = 100;
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = machine.getNextState(state, jointMove);
				int value = minimax(machine, nextState, role).value;
				if (value < min) {
					min = value;
				}
			}
			if (min > maxMove.value) {
				maxMove.value = min;
				maxMove.move = move;
			}
		}
		return maxMove;
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedProverStateMachine();
	}

	@Override
	public String getName() {
		return "Minimax";
	}
	
	@Override
	public DetailPanel getDetailPanel() {
		return new EggplantDetailPanel();
	}

}
