package player.gamer.statemachine.eggplant.heuristic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import player.gamer.statemachine.eggplant.heuristic.MobilityHeuristic.BranchingData;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class OpponentMobilityHeuristic extends MobilityHeuristic {

	public OpponentMobilityHeuristic(MobilityType type, int numPlayers) {
		this(type, numPlayers, (type == MobilityType.VAR_STEP) ? Math.max(2, numPlayers / 2) : 1);
	}
	
	public OpponentMobilityHeuristic(MobilityType type, int numPlayers, int deptLimit) {
		super(type, numPlayers, deptLimit);
	}
	
	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth, long endTime) {
		try {
			double avg = 0;
			switch (type) {
			case VAR_STEP:
				BranchingData data = getRelevantOpponentBranchingData(machine, state, role, 
						getDepthLimit(), endTime, samplesLimit());
				if (data.samples == 0) return (alpha + beta) / 2;
				avg = (double) data.total / data.samples;
				int ev = judgeRelevantMobility(avg);
				return (ev > 0) ? ev : (alpha + beta) / 2;
			default: 
				avg = machine.getLegalJointMoves(state).size() / machine.getLegalMoves(state, role).size();
				int eval = judgeMobility(avg, getIndex(depth + absDepth));
				/*System.out.println("opp res " + (eval > 0 ? eval : (alpha + beta) / 2) + " for bf " + avg 
						+ " against avg " + avgBranchingFactor(getIndex(depth + absDepth)) + " at depth " + (depth + absDepth) + "\n");*/
				return (eval > 0) ? eval : (alpha + beta) / 2;
			}
		} catch (MoveDefinitionException e) {
			e.printStackTrace();
		} catch (TransitionDefinitionException e) {
			e.printStackTrace();
		}
		return (alpha + beta) / 2;
	}
	
	private BranchingData getRelevantOpponentBranchingData(StateMachine machine, MachineState state, Role role, int depthLeft, long timeout, int limit) 
	throws MoveDefinitionException, TransitionDefinitionException {
		int samples = 0, total = 0;
		Queue<StateGroup> gQueue = new LinkedList<StateGroup>();
		gQueue.add(new StateGroup(state, depthLeft));
		while (!gQueue.isEmpty() && samples < limit) {
			if (System.currentTimeMillis() > timeout) break;
			StateGroup g = gQueue.poll();
			if (machine.isTerminal(state)) continue;
			List<List<Move>> joints = machine.getLegalJointMoves(g.state);
			List<Move> moves = machine.getLegalMoves(g.state, role);
			int sumMoves = joints.size(), roleMoves = moves.size(), quo = sumMoves / roleMoves;
			if (relevant(quo)) {
				samples++;
				total += quo;
				if (samples >= limit) break;
				continue;
			} else if (g.depth > 0) {
				for (List<Move> joint : joints) {
					if (System.currentTimeMillis() > timeout) break;
					MachineState nextState = machine.getNextState(g.state, joint);
					gQueue.add(new StateGroup(nextState, g.depth - 1));
				}
			}
		}
		return new BranchingData(samples, total);
	}

	@Override
	public void update(StateMachine machine, MachineState state, Role role, 
			int alpha, int beta, int depth, int absDepth) 
	throws MoveDefinitionException {
		int pmSize = machine.getLegalMoves(state, role).size();
		int oppSize = machine.getLegalJointMoves(state).size() / pmSize;
		updateAvg(oppSize, depth + absDepth);
	}

}
