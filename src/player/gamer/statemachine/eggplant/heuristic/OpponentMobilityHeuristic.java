package player.gamer.statemachine.eggplant.heuristic;

import java.util.*;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.Move;
import util.statemachine.exceptions.MoveDefinitionException;

public class OpponentMobilityHeuristic extends MobilityHeuristic {

	public OpponentMobilityHeuristic(MobilityType type, int numPlayers) {
		this(type, numPlayers, 1);
	}
	
	public OpponentMobilityHeuristic(MobilityType type, int numPlayers, int deptLimit) {
		super(type, numPlayers, deptLimit);
	}
	
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth) {
		try {
			int total = 0;
			List< List<Move> > moves = machine.getLegalJointMoves(state);
			for (List<Move> list : moves) {
				total += list.size();
			}
			total /= machine.getLegalMoves(state, role).size();
			return judgeMobility(total, getIndex(depth + absDepth));
		} catch (MoveDefinitionException e) {
			e.printStackTrace();
			return (alpha + beta) / 2;
		}
	}
	
	private int sumMoves(List<List<Move> > moves) {
		int total = 0;
		for (List<Move> list : moves) {
			total += list.size();
		}
		return total;
	}

	@Override
	public void update(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth) {
		try {
			int pmSize = machine.getLegalMoves(state, role).size();
			int oppSize = sumMoves(machine.getLegalJointMoves(state)) / pmSize;
			updateAverage(oppSize, depth + absDepth);
		} catch (MoveDefinitionException e) { /* meh */
		}
	}

}
