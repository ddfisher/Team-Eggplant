package player.gamer.statemachine.eggplant;

import java.util.Collections;
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

public class AlphaBetaGamer extends StateMachineGamer {
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

		ValuedMove result = alphabeta(getStateMachine(), getCurrentState(), getRole(), 0, 100);

		long stop = System.currentTimeMillis();
		notifyObservers(new EggplantMoveSelectionEvent(statesSearched, leafNodesSearched, stop - start, result.value, result.move));
		return result.move;
	}

	private ValuedMove alphabeta(StateMachine machine, MachineState state, Role role, int alpha, int beta) throws MoveDefinitionException, TransitionDefinitionException,
			GoalDefinitionException {
		statesSearched++;
		if (machine.isTerminal(state)) {
			leafNodesSearched++;
			return new ValuedMove(machine.getGoal(state, role), null);
		}

		ValuedMove maxMove = new ValuedMove(-1, null);
		List<Move> possibleMoves = machine.getLegalMoves(state, role);
		Collections.shuffle(possibleMoves); //TODO: Remove this line
		for (Move move : possibleMoves) {
			List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
			Collections.shuffle(jointMoves); //TODO: Remove this line
			int min = 100;
			int newBeta = beta;
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = machine.getNextState(state, jointMove);
				int value = alphabeta(machine, nextState, role, alpha, newBeta).value;
				if (value < min) {
					min = value;
					if (min <= alpha) break;
					if (min < newBeta) newBeta = min;
				}
			}
			if (min > maxMove.value) {
				maxMove.value = min;
				maxMove.move = move;
				if (maxMove.value >= beta) break;
				if (maxMove.value > alpha) alpha = maxMove.value;
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
		return "AlphaBeta";
	}
	
	@Override
	public DetailPanel getDetailPanel() {
		return new EggplantDetailPanel();
	}

}

